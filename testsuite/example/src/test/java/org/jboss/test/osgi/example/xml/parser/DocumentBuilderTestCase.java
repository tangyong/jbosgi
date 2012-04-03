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
package org.jboss.test.osgi.example.xml.parser;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.RepositorySupport;
import org.jboss.test.osgi.XMLParserSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A test that uses a DOM parser to read an XML document.
 *
 * @see http://www.osgi.org/javadoc/r4v41/org/osgi/util/xml/XMLParserActivator.html
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Jul-2009
 */
@RunWith(Arquillian.class)
public class DocumentBuilderTestCase {

    @Inject
    public BundleContext context;

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-xml-parser");
        archive.addClasses(RepositorySupport.class, XMLParserSupport.class);
        archive.addAsResource("xml/example-xml-parser.xml", "example-xml-parser.xml");
        archive.addAsManifestResource(RepositorySupport.BUNDLE_VERSIONS_FILE);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(DocumentBuilder.class, Document.class);
                builder.addImportPackages(XRequirementBuilder.class, Repository.class, Resource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testDOMParser() throws Exception {
        DocumentBuilder domBuilder = getDocumentBuilder();
        URL resURL = bundle.getResource("example-xml-parser.xml");
        Document dom = domBuilder.parse(resURL.openStream());
        assertNotNull("Document not null", dom);

        Element root = dom.getDocumentElement();
        assertEquals("root", root.getLocalName());

        Node child = root.getFirstChild();
        assertEquals("child", child.getLocalName());
        assertEquals("content", child.getTextContent());
    }

    private DocumentBuilder getDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = XMLParserSupport.provideDocumentBuilderFactory(context, bundle);
        factory.setValidating(false);
        return factory.newDocumentBuilder();
    }
}