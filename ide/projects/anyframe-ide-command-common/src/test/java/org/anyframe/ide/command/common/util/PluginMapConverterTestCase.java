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
package org.anyframe.ide.command.common.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.anyframe.ide.command.common.plugin.DependentPlugin;
import org.anyframe.ide.command.common.plugin.Include;
import org.anyframe.ide.command.common.plugin.PluginInfo;
import org.anyframe.ide.command.common.plugin.PluginResource;
import org.apache.commons.collections.map.ListOrderedMap;

/**
 * TestCase Name : PluginMapConverterTestCase <br>
 * <br>
 * [Description] : Test for plugin map convertor<br>
 * [Main Flow]
 * <ul>
 * <li>#-1 Positive Case : convert PluginInfo object to plugin-catalog-optional.xml file</li>
 * <li>#-2 Positive Case : convert plugin.xml file to PluginInfo object</li>
 * <li>#-3 Positive Case : convert PluginInfo object to plugin.xml file</li>
 * </ul>
 */
public class PluginMapConverterTestCase extends TestCase {
	
	/**
	 * [Flow #-1] Positive Case : convert PluginInfo object to plugin-catalog-optional.xml file
	 * 
	 * @throws Exception
	 */
	public void testPluginCatalogXMLToPluginObject() throws Exception {
		File file = new File(
				"./src/test/resources/.anyframe/plugin-catalog-optional.xml");
		Map<String, PluginInfo> plugins = (Map<String, PluginInfo>) FileUtil
				.getObjectFromXML(file);

		PluginInfo pluginInfo = plugins.get("cxf-jaxrs");
		assertNotNull("fail to convert xml to plugin object.", pluginInfo);

		assertEquals("fail to convert xml to plugin object - latestVersion.",
				"1.0.0", pluginInfo.getLatestVersion());
		assertEquals("fail to convert xml to plugin object - description.",
				"cxf-jaxrs plugin", pluginInfo.getDescription());
		assertEquals("fail to convert xml to plugin object - groupId.",
				"org.anyframe.plugin", pluginInfo.getGroupId());
		assertEquals("fail to convert xml to plugin object - artifactId.",
				"anyframe-cxf-jaxrs-pi", pluginInfo.getArtifactId());
		assertEquals("fail to convert xml to plugin object - versions.",
				"1.0.0", pluginInfo.getVersions().get(0));
	}

	/**
	 * [Flow #-2] Positive Case : convert plugin.xml file to PluginInfo object
	 * 
	 * @throws Exception
	 */
	public void testPluginXMLToPluginObject() throws Exception {

		File file = new File(
				"./src/test/resources/plugin/cxf-jaxrs/META-INF/anyframe/plugin.xml");
		PluginInfo pluginInfo = (PluginInfo) FileUtil.getObjectFromXML(file);

		assertEquals("fail to convert xml to plugin object - name.",
				"cxf-jaxrs", pluginInfo.getName());
		assertEquals("fail to convert xml to plugin object - groupId.",
				"org.anyframe.plugin", pluginInfo.getGroupId());
		assertEquals("fail to convert xml to plugin object - artifactId.",
				"anyframe-cxf-jaxrs-pi", pluginInfo.getArtifactId());
		assertEquals("fail to convert xml to plugin object - version.",
				"1.0.0", pluginInfo.getVersion());
		assertEquals("fail to convert xml to plugin object - dependencies.",
				"1.0.0>=*", pluginInfo.getDependentPlugins().get(0)
						.getVersion());
		assertTrue("fail to convert xml to plugin object - samples.",
				pluginInfo.hasSamples());
	}

	/**
	 * [Flow #-3] Positive Case : convert PluginInfo object to plugin.xml file
	 * 
	 * @throws Exception
	 */
	public void testPluginObjectToPluginXML() throws Exception {
		Map<String, PluginInfo> result = new ListOrderedMap();

		PluginInfo pluginInfo = new PluginInfo();
		pluginInfo.setName("test");
		pluginInfo.setDescription("test plugin");
		pluginInfo.setLatestVersion("1.0.0");

		List<String> versions = new ArrayList<String>();
		versions.add("0.5");
		versions.add("0.6");
		versions.add("0.7");
		versions.add("1.0");
		pluginInfo.setVersions(versions);

		List<DependentPlugin> dependencies = new ArrayList<DependentPlugin>();
		DependentPlugin dependency = new DependentPlugin();
		dependency.setName("test1");
		dependency.setVersion("1.0.0 > *");
		dependencies.add(dependency);
		dependency = new DependentPlugin();
		dependency.setName("test2");
		dependency.setVersion("1.0.0 > *");
		dependencies.add(dependency);
		pluginInfo.setDependentPlugins(dependencies);

		List<PluginResource> resources = new ArrayList<PluginResource>();
		PluginResource resource = new PluginResource();
		resource.setDir("src/main/java");
		resource.setFiltered(true);

		List<Include> includes = new ArrayList<Include>();
		Include include = new Include();
		include.setName("**/*.java");
		includes.add(include);
		include = new Include();
		include.setName("**/*.java1");
		includes.add(include);

		resource.setIncludes(includes);
		resources.add(resource);

		pluginInfo.setResources(resources);

		result.put("test", pluginInfo);

		File temporaryFile = new File("./temp/plugin/plugin-test.xml");
		temporaryFile.getParentFile().mkdirs();
		temporaryFile.createNewFile();

		FileUtil.getObjectToXML(result, temporaryFile);
	}

}
