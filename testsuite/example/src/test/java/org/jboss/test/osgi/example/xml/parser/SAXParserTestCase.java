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
import org.osgi.framework.resource.Resource;
import org.osgi.service.repository.Repository;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.inject.Inject;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * A test that uses a SAX parser to read an XML document.
 *
 * @see http://www.osgi.org/javadoc/r4v41/org/osgi/util/xml/XMLParserActivator.html
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Jul-2009
 */
@RunWith(Arquillian.class)
public class SAXParserTestCase {

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
                builder.addImportPackages(SAXParser.class, DefaultHandler.class, SAXException.class);
                builder.addImportPackages(XRequirementBuilder.class, Repository.class, Resource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSAXParser() throws Exception {
        bundle.start();

        SAXParser saxParser = getSAXParser();
        URL resURL = bundle.getResource("example-xml-parser.xml");

        SAXHandler saxHandler = new SAXHandler();
        saxParser.parse(resURL.openStream(), saxHandler);
        assertEquals("content", saxHandler.getContent());
    }

    private SAXParser getSAXParser() throws Exception {
        SAXParserFactory factory = XMLParserSupport.provideSAXParserFactory(context, bundle);
        factory.setValidating(false);

        SAXParser saxParser = factory.newSAXParser();
        return saxParser;
    }

    static class SAXHandler extends DefaultHandler {
        private String content;

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            content = new String(ch, start, length);
        }

        public String getContent() {
            return content;
        }
    }
}