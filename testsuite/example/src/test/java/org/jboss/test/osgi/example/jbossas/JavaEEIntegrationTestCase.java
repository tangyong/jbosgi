/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.test.osgi.example.jbossas;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.osgi.spi.ManifestBuilder;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.osgi.HttpSupport;
import org.jboss.test.osgi.example.jbossas.api.PaymentService;
import org.jboss.test.osgi.example.jbossas.ejb3.SimpleStatelessSessionBean;
import org.jboss.test.osgi.example.jbossas.service.PaymentActivator;
import org.jboss.test.osgi.example.jbossas.webapp.SimpleBeanClientServlet;
import org.jboss.test.osgi.example.jbossas.webapp.SimpleClientServlet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * A test that deployes two bundles
 *
 * - example-javaee-api
 * - example-javaee-service
 *
 * and two JavaEE archives
 *
 * - example-javaee-ejb3
 * - example-javaee-servlet
 *
 * It demonstrates how JavaEE components can access OSGi services.
 *
 * @author thomas.diesler@jboss.com
 * @since 01-Feb-2012
 */
@RunWith(Arquillian.class)
public class JavaEEIntegrationTestCase {

    private static final String API_DEPLOYMENT_NAME = "example-javaee-api";
    private static final String SERVICE_DEPLOYMENT_NAME = "example-javaee-service";
    private static final String EJB3_DEPLOYMENT_NAME = "example-javaee-ejb3";
    private static final String SERVLET_DEPLOYMENT_NAME = "example-javaee-servlet.war";

    @ArquillianResource
    public Deployer deployer;

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-javaee");
        archive.addClasses(HttpSupport.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testServletAccess() throws Exception {
        
        // Deploy the bundle that provides the API
        deployer.deploy(API_DEPLOYMENT_NAME);
        
        // Deploy the bundle that provides the service
        InputStream input = deployer.getDeployment(SERVICE_DEPLOYMENT_NAME);
        Bundle bundle = context.installBundle(SERVICE_DEPLOYMENT_NAME, input);
        try {
            // Start the service bundle
            bundle.start();

            // Deploy the EJB3 and Servlet clients
            deployer.deploy(EJB3_DEPLOYMENT_NAME);
            deployer.deploy(SERVLET_DEPLOYMENT_NAME);
            
            String response = getHttpResponse("/sample/simple?account=kermit&amount=100", 2000);
            assertEquals("Calling PaymentService: Charged $100.0 to account 'kermit'", response);

            response = getHttpResponse("/sample/ejb?account=kermit&amount=100", 2000);
            assertEquals("Calling SimpleStatelessSessionBean: Charged $100.0 to account 'kermit'", response);
            
            deployer.undeploy(SERVLET_DEPLOYMENT_NAME);
            deployer.undeploy(EJB3_DEPLOYMENT_NAME);
        } finally {
            bundle.uninstall();
        }
        deployer.undeploy(API_DEPLOYMENT_NAME);
    }

    @Deployment(name = API_DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getApiArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, API_DEPLOYMENT_NAME);
        archive.addClasses(PaymentService.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addExportPackages(PaymentService.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = SERVICE_DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getServiceArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, SERVICE_DEPLOYMENT_NAME);
        archive.addClasses(PaymentActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(PaymentActivator.class);
                builder.addImportPackages(BundleActivator.class, Logger.class);
                builder.addImportPackages(PaymentService.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = EJB3_DEPLOYMENT_NAME, managed = false, testable = false)
    public static JavaArchive getEjbArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, EJB3_DEPLOYMENT_NAME);
        archive.addClasses(SimpleStatelessSessionBean.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                String osgidep = "org.osgi.core,org.jboss.osgi.framework";
                String apidep = ",deployment." + API_DEPLOYMENT_NAME + ":0.0.0";
                builder.addManifestHeader("Dependencies", osgidep + apidep);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = SERVLET_DEPLOYMENT_NAME, managed = false, testable = false)
    public static WebArchive getServletArchive() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SERVLET_DEPLOYMENT_NAME);
        archive.addClasses(SimpleBeanClientServlet.class, SimpleClientServlet.class);
        archive.addAsWebInfResource("jbossas/webapp/jboss-web.xml", "jboss-web.xml");
        // [SHRINKWRAP-278] WebArchive.setManifest() results in WEB-INF/classes/META-INF/MANIFEST.MF
        archive.add(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                String osgidep = "org.osgi.core,org.jboss.osgi.framework";
                String apidep = ",deployment." + API_DEPLOYMENT_NAME + ":0.0.0";
                String ejbdep = ",deployment." + EJB3_DEPLOYMENT_NAME;
                builder.addManifestHeader("Dependencies", osgidep + apidep + ejbdep);
                return builder.openStream();
            }
        }, JarFile.MANIFEST_NAME);
        return archive;
    }

    private String getHttpResponse(String reqPath, int timeout) throws IOException {
        return HttpSupport.getHttpResponse("localhost", 8080, reqPath, timeout);
    }
}