/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.anyframe.ide.eclipse.core.wizards;

import java.io.File;
import java.io.FileInputStream;

import org.anyframe.ide.eclipse.core.config.JdbcType;
import org.anyframe.ide.eclipse.core.util.XmlFileUtil;
import org.anyframe.ide.eclipse.core.wizards.NewApplicationWizardJDBCPage;

import junit.framework.TestCase;

/**
 * This is a NewApplicationWizardJDBCPageTestCase class.
 * 
 * @author Changje Kim
 * @author Sooyeon Park
 */
public class NewApplicationWizardJDBCPageTestCase extends TestCase {

    private String anyframeHome = "./src/test/resources/";

    public void testSetGenHome() throws Exception {
        NewApplicationWizardJDBCPage jdbcPage =
            new NewApplicationWizardJDBCPage("NewApplicationWizardJDBCPage");

        jdbcPage.setAnyframeHome(anyframeHome);
        assertNotNull(jdbcPage.getAnyframeHome());
    }

    public void testGetJdbcTypes() throws Exception {
        File jdbcConfigFile = new File(anyframeHome + "/db/jdbc.config");
        java.util.List<JdbcType> jdbcTypes;

        if (jdbcConfigFile.exists()) {
            jdbcTypes =
                (java.util.List<JdbcType>) XmlFileUtil
                    .getObjectFromInputStream(new FileInputStream(
                        jdbcConfigFile));

            assertNotNull(jdbcTypes);

            for (JdbcType jdbcType : jdbcTypes) {
                jdbcType.getType();
                System.out.println(jdbcType.getType());
            }
        }
    }
}
