/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/workplace/TestWorkplace.java,v $
 * Date   : $Date: 2004/12/08 14:30:29 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace;

import org.opencms.test.OpenCmsTestCase;
import org.opencms.test.OpenCmsTestProperties;
import org.opencms.workplace.commons.CmsGallery;
import org.opencms.workplace.commons.CmsGalleryDownloads;
import org.opencms.workplace.commons.CmsGalleryHtmls;
import org.opencms.workplace.commons.CmsGalleryImages;
import org.opencms.workplace.commons.CmsGalleryLinks;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.1 $
 * 
 * @since 5.5.4
 */
public class TestWorkplace extends OpenCmsTestCase {

    /**
     * Default JUnit constructor.<p>
     * 
     * @param arg0 JUnit parameters
     */
    public TestWorkplace(String arg0) {

        super(arg0);
    }

    /**
     * Test suite for this test class.<p>
     * 
     * @return the test suite
     */
    public static Test suite() {

        OpenCmsTestProperties.initialize(org.opencms.test.AllTests.TEST_PROPERTIES_PATH);

        TestSuite suite = new TestSuite();
        suite.setName(TestWorkplace.class.getName());

        suite.addTest(new TestWorkplace("testGalleryClassCreation"));

        TestSetup wrapper = new TestSetup(suite) {

            protected void setUp() {

                setupOpenCms("simpletest", "/sites/default/");
            }

            protected void tearDown() {

                removeOpenCms();
            }
        };

        return wrapper;
    }

    /**
     * Tests dynamic creation of gallery classes.<p>
     * 
     * @throws Exception in case the test fails
     */
    public void testGalleryClassCreation() throws Exception {

        CmsGallery gallery;

        gallery = CmsGallery.createInstance("imagegallery", null);
        assertEquals(gallery.getClass().getName(), CmsGalleryImages.class.getName());
        assertEquals("imagegallery", gallery.getGalleryTypeName());
        assertEquals(15, gallery.getGalleryTypeId());        
        
        gallery = CmsGallery.createInstance("downloadgallery", null);
        assertEquals(gallery.getClass().getName(), CmsGalleryDownloads.class.getName());
        assertEquals("downloadgallery", gallery.getGalleryTypeName());
        assertEquals(18, gallery.getGalleryTypeId());     
        
        gallery = CmsGallery.createInstance("linkgallery", null);
        assertEquals(gallery.getClass().getName(), CmsGalleryLinks.class.getName());
        assertEquals("linkgallery", gallery.getGalleryTypeName());
        assertEquals(16, gallery.getGalleryTypeId());     
        
        gallery = CmsGallery.createInstance("htmlgallery", null);
        assertEquals(gallery.getClass().getName(), CmsGalleryHtmls.class.getName());
        assertEquals("htmlgallery", gallery.getGalleryTypeName());
        assertEquals(17, gallery.getGalleryTypeId());     
        
        boolean error = true;
        try {
            CmsGallery.createInstance("unknowngallery", null);
        } catch (RuntimeException e) {
            error = false;
        }
        if (error) {
            fail("Unknow gallery instance class could be created");
        }

        error = true;
        try {
            CmsGallery.createInstance(null, null);
        } catch (RuntimeException e) {
            error = false;
        }
        if (error) {
            fail("Null gallery instance class could be created");
        }
    }
}