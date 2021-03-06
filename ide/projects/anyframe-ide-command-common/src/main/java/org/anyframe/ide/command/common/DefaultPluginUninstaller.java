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
package org.anyframe.ide.command.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipFile;

import org.anyframe.ide.command.common.plugin.Exclude;
import org.anyframe.ide.command.common.plugin.Include;
import org.anyframe.ide.command.common.plugin.PluginInfo;
import org.anyframe.ide.command.common.plugin.PluginResource;
import org.anyframe.ide.command.common.util.CommonConstants;
import org.anyframe.ide.command.common.util.ConfigXmlUtil;
import org.anyframe.ide.command.common.util.DBUtil;
import org.anyframe.ide.command.common.util.FileUtil;
import org.anyframe.ide.command.common.util.JdbcOption;
import org.anyframe.ide.command.common.util.ObjectUtil;
import org.anyframe.ide.command.common.util.ProjectConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.common.Constants;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;

/**
 * This is a DefaultPluginUninstaller class. This class is for uninstalling a
 * plugin based on selected plugin name
 * 
 * @plexus.component 
 *                   role="org.anyframe.ide.command.common.DefaultPluginUninstaller"
 * @author Jeryeon Kim
 */
public class DefaultPluginUninstaller extends AbstractLogEnabled implements PluginUninstaller {
	/**
	 * @plexus.requirement 
	 *                     role="org.anyframe.ide.command.common.DefaultPluginPomManager"
	 */
	private DefaultPluginPomManager pluginPomManager;

	/**
	 * @plexus.requirement 
	 *                     role="org.anyframe.ide.command.common.DefaultPluginArtifactManager"
	 */
	private DefaultPluginArtifactManager pluginArtifactManager;

	/**
	 * @plexus.requirement 
	 *                     role="org.anyframe.ide.command.common.DefaultPluginCatalogManager"
	 */
	PluginCatalogManager pluginCatalogManager;

	/**
	 * @plexus.requirement 
	 *                     role="org.anyframe.ide.command.common.DefaultPluginInfoManager"
	 */
	PluginInfoManager pluginInfoManager;

	/**
	 * delegates to {@link #uninstallPlugin} for the actual uninstalling.
	 * 
	 * @param request
	 *            information includes maven repository, db settings, plugin
	 * @param baseDir
	 *            the path of current project
	 * @param pluginName
	 *            plugin name to be uninstalled
	 * @param excludes
	 *            files which exclude from removing
	 * @param encoding
	 *            file encoding style
	 * @param pomHandling
	 *            whether to handle pom file
	 * @throws CommandException
	 */
	public void uninstall(ArchetypeGenerationRequest request, String baseDir, String pluginName, String excludes, String encoding, boolean pomHandling)
			throws CommandException {
		uninstall(request, baseDir, pluginName, excludes, encoding, pomHandling, false);
	}

	/**
	 * uninstall a plugin
	 * 
	 * @param request
	 *            information includes maven repository, db settings, plugin
	 * @param baseDir
	 *            the path of current project
	 * @param pluginName
	 *            plugin name to be uninstalled
	 * @param excludes
	 *            file list which exclude from removing
	 * @param encoding
	 *            file encoding style
	 * @param pomHandling
	 *            whether to handle pom file
	 * @param ignoreDependency
	 *            ignore dependency plugins
	 * @throws CommandException
	 */
	public void uninstall(ArchetypeGenerationRequest request, String baseDir, String pluginName, String excludes, String encoding,
			boolean pomHandling, boolean ignoreDependency) throws CommandException {
		getLogger().debug(DefaultPluginUninstaller.class.getName() + " execution start.");

		ClassLoader old = Thread.currentThread().getContextClassLoader();

		try {
			// 1. check validation about current project
			ProjectConfig projectConfig = checkProject(baseDir);

			// 2. process comma-separator.
			String[] pluginNames = handleCommaSeparatedPluginNames(pluginName);

			// prepare directory for backup
			File backupDirectory = makeBackupDirectory(baseDir);

			// 3. uninstall plugins
			for (String uninstallPluginName : pluginNames) {
				uninstallPluginName = uninstallPluginName.trim();

				try {
					PluginInfo pluginInfo = pluginInfoManager.getInstalledPluginInfo(request, baseDir, uninstallPluginName);

					if (pluginInfo != null) {
						uninstallPlugin(request, baseDir, pluginInfo, excludes, encoding, pomHandling, projectConfig, backupDirectory, ignoreDependency);
					} else {
						throw new CommandException("Can't uninstall the '" + uninstallPluginName + "' plugin. The reason is '" + uninstallPluginName
								+ "' plugin maybe not installed or already uninstalled.");
					}
				} catch (Exception e) {
					// can't stop uninstalling other plugins.
					if (e instanceof CommandException) {
						getLogger().error(e.getMessage());
					} else {
						getLogger().error("Can't uninstall the '" + uninstallPluginName + "' plugin. The reason is a '" + e.getMessage() + "'.");
					}
				}
			}
		} catch (Exception e) {
			if (e instanceof CommandException)
				throw (CommandException) e;

			throw new CommandException("Error occurred in uninstalling '" + pluginName + "' plugin. The reason is a '" + e.getMessage() + "'.");
		} finally {
			Thread.currentThread().setContextClassLoader(old);
		}
	}

	/**
	 * validate input plugin names to uninstall and prepare the uninstalling
	 * 
	 * @param request
	 *            information includes maven repository, db settings, plugin
	 * @param baseDir
	 *            the path of current project
	 * @param pluginInfo
	 *            plugin information (groupId, artifactId, version, ...)
	 * @param excludes
	 *            files which exclude from removing
	 * @param encoding
	 *            file encoding style
	 * @param pomHandling
	 *            whether to handle pom file
	 * @param projectConfig
	 *            project configuration
	 * @param backupDir
	 *            directory for backup
	 * @param ignoreDependency
	 *            ignore dependency plugins
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void uninstallPlugin(ArchetypeGenerationRequest request, String baseDir, PluginInfo pluginInfo, String excludes, String encoding,
			boolean pomHandling, ProjectConfig projectConfig, File backupDir, boolean ignoreDependency) throws Exception {

		// 1. check validation about selected plugin
		checkPlugin(request, baseDir, pluginInfo, ignoreDependency);

		// 2. get jar files of installed plugins
		Map<String, File> installedPluginJars = pluginInfoManager.getInstalledPluginJars(request, baseDir);

		// 3. get a plugin jar file to remove
		File pluginJar = (File) installedPluginJars.get(pluginInfo.getName());

		// 4. set classloader for VelocityComponent can load templates inside
		// plugin library
		ClassLoader pluginJarLoader = pluginArtifactManager.getArchetypeJarLoader(pluginJar);
		Thread.currentThread().setContextClassLoader(pluginJarLoader);

		// 5. get interceptor of plugin
		Class interceptor = getInterceptor(request, pluginJarLoader, pluginJar, pluginInfo);

		// 6. invokeInterceptor
		invokeInterceptor(baseDir, pluginInfo.getName(), pluginJar, interceptor, "preUninstall");

		// 7. uninstall
		process(request, baseDir, pluginInfo, installedPluginJars, pluginJar, excludes, encoding, pomHandling, projectConfig, backupDir);

		// 8. invokeInterceptor
		invokeInterceptor(baseDir, pluginInfo.getName(), pluginJar, interceptor, "postUninstall");

		System.out.println("'" + pluginInfo.getName() + " " + pluginInfo.getVersion() + "' plugin is uninstalled successfully.");
	}

	/**
	 * Check if input pluginName is null or empty string. And process comma
	 * separator.
	 * 
	 * @param pluginName
	 *            the input plugin names with comma-separator
	 * @return the array of plugin name
	 * @throws Exception
	 */
	private String[] handleCommaSeparatedPluginNames(String pluginName) throws Exception {

		// 1. process comma-separator.
		String[] pluginNames = new String[] { pluginName };
		if (pluginName.indexOf(",") != -1) {
			pluginNames = pluginName.split(",");
		}

		return pluginNames;
	}

	/**
	 * Check current project information
	 * 
	 * @param baseDir
	 *            the path of current project
	 * @return the ProjectConfig
	 * @throws Exception
	 */
	private ProjectConfig checkProject(String baseDir) throws Exception {
		getLogger().debug("Call checkProject() of DefaultPluginUninstaller");

		// 1. get a project Configuration
		String configFile = ConfigXmlUtil.getCommonConfigFile(baseDir);
		ProjectConfig projectConfig = ConfigXmlUtil.getProjectConfig(configFile);

		// 2. get a plugin-installed.xml file which includes installation
		// information
		File pluginsXMLFile = new File(new File(baseDir) + CommonConstants.METAINF, CommonConstants.PLUGIN_INSTALLED_FILE);

		// 3. if plugin-installed.xml file doesn't exist, can't proceed
		if (!pluginsXMLFile.exists()) {
			throw new CommandException("Can not find a '" + pluginsXMLFile.getAbsolutePath() + "' file. Please check a location of your project.");
		}

		getLogger().debug("Current target directory is a " + baseDir);

		return projectConfig;
	}

	/**
	 * Check if current plugin can be uninstalled
	 * 
	 * @param request
	 *            information includes maven repository, db settings, plugin
	 * @param baseDir
	 *            the path of current project
	 * @param pluginInfo
	 *            plugin information (groupId, artifactId, version, ...)
	 * @param ignoreDependency
	 *            ignore dependency plugins
	 * @throws Exception
	 */
	private void checkPlugin(ArchetypeGenerationRequest request, String baseDir, PluginInfo pluginInfo, boolean ignoreDependency) throws Exception {
		String pluginName = pluginInfo.getName();
		String pluginVersion = pluginInfo.getVersion();
		getLogger().debug("Call checkPlugin() of DefaultPluginUninstaller - pluginName : " + pluginName + ", pluginVersion : " + pluginVersion);

		// 1. check if plugin is installed.
		Map<String, PluginInfo> installedPlugins = pluginInfoManager.getInstalledPlugins(baseDir);

		if (!installedPlugins.containsKey(pluginInfo.getName())) {
			throw new CommandException("Can't uninstall the '" + pluginName + " " + pluginVersion + "' plugin. The reason is '" + pluginName + " "
					+ pluginVersion + "' plugin maybe not installed or already uninstalled.");
		}

		Map<String, String> dependedPlugins = pluginInfoManager.getDependedPlugins(request, baseDir, pluginInfo);

		// 2. is there a plugin that uses this plugin?
		if (!ignoreDependency && !dependedPlugins.isEmpty()) {
			StringBuffer pluginNames = new StringBuffer();
			int i = 0;
			for (String dependedPluginName : dependedPlugins.keySet()) {
				if (i != 0)
					pluginNames.append(", ");
				pluginNames.append(dependedPluginName);
				i++;
			}
			throw new CommandException("Can't uninstall the '" + pluginName + " " + pluginVersion
					+ "' plugin. The reason is there are plugins which depends on a '" + pluginName + " " + pluginVersion
					+ "' plugin. Please try uninstall '" + pluginNames.toString() + "' plugins before.");
		}
	}

	/**
	 * Get interceptor from artifactClassLoader
	 * 
	 * @param request
	 *            information includes maven repository, db settings, plugin
	 * @param pluginJarLoader
	 *            the classloader for current plugin jar
	 * @param pluginJar
	 *            plugin jar file to be removed
	 * @param pluginInfo
	 *            plugin information (groupId, artifactId, version, ...)
	 * @return the interceptor class, or <code>null</code> if no interceptor
	 *         could be found
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private Class getInterceptor(ArchetypeGenerationRequest request, ClassLoader pluginJarLoader, File pluginJar, PluginInfo pluginInfo)
			throws Exception {

		Class interceptor = null;

		// 1. get zipfile from plugin jar file
		ZipFile pluginZip = pluginArtifactManager.getArchetypeZipFile(pluginJar);

		if (pluginInfo.getInterceptor() != null) {

			URL[] originalUrls = ((URLClassLoader) pluginJarLoader).getURLs();
			Model model = pluginPomManager.readPom(pluginZip);

			URLClassLoader interceptorLoader = pluginArtifactManager.makeArtifactClassLoader(request, model.getGroupId(), model.getArtifactId(),
					model.getVersion(), originalUrls);

			interceptor = ObjectUtil.loadClass(interceptorLoader, pluginJar, pluginInfo.getInterceptor().getClassName().trim());
		}

		return interceptor;
	}

	/**
	 * process the actual plugin uninstalling.
	 * 
	 * @param request
	 *            information includes maven repository, db settings, plugin
	 * @param baseDir
	 *            target folder
	 * @param pluginInfo
	 *            plugin information (groupId, artifactId, version, ...)
	 * @param installedPluginJars
	 *            installed plugin jar files
	 * @param pluginJar
	 *            plugin jar file to be removed
	 * @param excludes
	 *            files which exclude from removing
	 * @param encoding
	 *            file encoding style
	 * @param pomHandling
	 *            whether to handle pom file
	 * @param projectConfig
	 *            project configuration
	 * @param backupDir
	 *            directory for backup
	 */
	public void process(ArchetypeGenerationRequest request, String baseDir, PluginInfo pluginInfo, Map<String, File> installedPluginJars,
			File pluginJar, String excludes, String encoding, boolean pomHandling, ProjectConfig projectConfig, File backupDir) throws Exception {

		// remove a plugin to be removed from installed plugins
		installedPluginJars.remove(pluginInfo.getName());

		File springPluginJar = null;
		PluginInfo springPlugin = null;
		Properties springProperties = null;

		// get spring plugin's pom properties to handle '${spring.version}'
		Map<String, PluginInfo> installedPlugins = pluginInfoManager.getInstalledPlugins(baseDir);

		if (installedPlugins != null && installedPlugins.containsKey(CommonConstants.SPRING_PLUGIN)) {
			springPlugin = installedPlugins.get(CommonConstants.SPRING_PLUGIN);
			if (springPlugin != null)
				springPluginJar = pluginInfoManager.getPluginFile(request, springPlugin.getGroupId(), springPlugin.getArtifactId(),
						springPlugin.getVersion());

			Model model = pluginPomManager.getPluginPom(springPluginJar);
			springProperties = model.getProperties();
		} else {
			springProperties = new Properties();
		}

		if (pomHandling) {
			// process pom file
			processPom(baseDir, installedPluginJars, pluginJar, backupDir, springProperties);
			addProcessPom(baseDir, pluginJar);
		} else {
			// remove dependent libraries
			processDependencyLibs(baseDir, pluginInfo.getName(), installedPluginJars, pluginJar,
					springProperties, backupDir);
			addDependencyLibs(request, baseDir, pluginJar);
		}

		// remove generated folder, generated xml files
		ArrayList<String> excludeList = new ArrayList<String>();

		String[] exclude = excludes.split(",");
		for (int i = 0; i < exclude.length; i++) {
			excludeList.add(exclude[i]);
		}

		if (pluginInfo.getName().equals(CommonConstants.HIBERNATE_PLUGIN)) {
			excludeList.add(CommonConstants.HIBERNATE_CFG_XML_FILE);
		}

		// get plugin names to protect resources of other plugins
		// (cxf-jaxws, cxf-jaxrs, simpleweb-vo, simpleweb-map, query-ria...)
		List<String> pluginNames = new ArrayList<String>();
		Map<String, PluginInfo> pluginsMap = pluginCatalogManager.getPlugins(request);

		if (pluginsMap != null) {
			Collection<PluginInfo> plugins = pluginsMap.values();

			for (PluginInfo plugin : plugins) {
				if (!plugin.getName().equals(pluginInfo.getName())) {
					pluginNames.add(plugin.getName());
				}
			}
		}

		List<String> fileNames = FileUtil.resolveFileNames(pluginJar);

		ZipFile pluginZip = pluginArtifactManager.getArchetypeZipFile(pluginJar);

		processTemplates(baseDir, backupDir, fileNames, pluginInfo, pluginZip, projectConfig);

		// remove a link from index file
		processWelcomeFile(baseDir, pluginInfo.getName());

		// process file about transaction configuration
		if (pluginInfo.getName().equals(CommonConstants.HIBERNATE_PLUGIN))
			processTransactionFile(baseDir);

		// process file about messageSource configuration
		if (pluginInfo.getName().equals(CommonConstants.I18N_PLUGIN))
			processMessageFile(baseDir);

		// drop table, delete data to DB
		processInitialData(new File(baseDir), fileNames, pluginInfo.getName(), pluginZip, encoding, projectConfig);

		// 10. update plugin-installed.xml
		updateInstallationInfo(baseDir, pluginInfo.getName());
	}

	/**
	 * invoke method of interceptor class for current plugin
	 * 
	 * @param baseDir
	 *            the path of current project
	 * @param pluginName
	 *            the name of plugin
	 * @param pluginJar
	 *            the plugin jar file
	 * @param interceptor
	 *            the interceptor class
	 * @param interceptorMethodName
	 *            the name of method to invoke
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void invokeInterceptor(String baseDir, String pluginName, File pluginJar, Class interceptor, String interceptorMethodName)
			throws Exception {
		if (interceptor != null) {
			getLogger().debug("Before calling invokeInterceptor() of " + interceptor.getName());
			try {
				ObjectUtil.invokeMethod(interceptor, interceptorMethodName, baseDir, pluginJar);
				getLogger().debug("Invoke " + interceptor + "." + interceptorMethodName + "() of plugin '" + pluginName + "' successfully.");
			} catch (Exception e) {
				getLogger().warn("Invoking of a " + interceptor.getName() + " is skipped. The reason is a '" + e.getMessage() + "'.");
				return;
			}
		}
	}

	/**
	 * in case of uninstalling using maven, find dependencies to be removed
	 * 
	 * @param baseDir
	 *            the path of current project
	 * @param installedPluginJars
	 *            installed plugin jar files
	 * @param pluginJar
	 *            plugin jar file to be removed
	 * @param backupDir
	 *            directory for backup
	 * @param properties
	 *            properties of pom in current project
	 */
	public void processPom(String baseDir, Map<String, File> installedPluginJars, File pluginJar, File backupDir, Properties properties) {
		getLogger().debug("Call processPom() of DefaultPluginUninstaller");

		File pomFile = new File(new File(baseDir), Constants.ARCHETYPE_POM);

		try {
			// pom.xml
			String targetPath = new File(baseDir, Constants.ARCHETYPE_POM).getCanonicalPath()
					.substring(new File(baseDir).getCanonicalPath().length());

			FileUtils.copyFile(new File(baseDir, Constants.ARCHETYPE_POM), new File(backupDir, targetPath));
			getLogger().debug("pom.xml file backup is complete.");

			pluginPomManager.removePomDependencies(pomFile, installedPluginJars, pluginJar, properties);
		} catch (Exception e) {
			getLogger().warn("Processing a pom.xml of current project is skipped. The reason is a '" + e.getMessage() + "'.");
		}
	}

	public void addProcessPom(String baseDir, File pluginJar) throws Exception {
		getLogger().debug("Call addProcessPom() of DefaultPluginUninstaller");

		List<String> fileNames = FileUtil.resolveFileNames(pluginJar);

		List<String> addPomFiles = FileUtil.findFiles(fileNames, CommonConstants.PLUGIN_RESOURCES, "**\\" + CommonConstants.ARCHETYPE_REMOVE_POM,
				null);

		if (addPomFiles.size() > 0) {
			try {
				File temporaryPomFile = new File(baseDir, "remove-pom.tmp");
				temporaryPomFile.getParentFile().mkdirs();

				InputStream is = pluginInfoManager.getPluginResource(addPomFiles.get(0), pluginJar);

				IOUtil.copy(is, new FileOutputStream(temporaryPomFile));

				pluginPomManager.mergePom(new File(baseDir, Constants.ARCHETYPE_POM), temporaryPomFile);

				FileUtil.deleteFile(temporaryPomFile);
			} catch (Exception e) {
				getLogger().debug("Processing a remove-pom.xml of current project is skipped. The reason is a '" + e.getMessage() + "'.");
			}
		} else {
			getLogger().debug(
					"Merging current pom file with that of plugin is skipped. The reason is a pom.xml in " + pluginJar.getName() + " doesn't exist.");
		}

	}

	/**
	 * find dependencies to be removed
	 * 
	 * @param installedPluginJars
	 *            installed plugin jar files
	 * @param pluginJar
	 *            plugin jar file to be removed
	 * @param properties
	 *            properties of pom in current project
	 * @return jar file names to be removed
	 * @throws Exception
	 */
	public List<String> findRemoveDependencies(Map<String, File> installedPluginJars, File pluginJar, Properties properties) throws Exception {

		getLogger().debug("Call findRemoveDependencies() of DefaultPluginUninstaller");
		// 1. find dependencies to be removed
		try {
			List<Dependency> removes = pluginPomManager.findRemovedDependencies(installedPluginJars, pluginJar, properties);

			// 2. make files to be removed
			List<String> removeFileNames = new ArrayList<String>();

			for (Dependency dependency : removes) {
				removeFileNames.add(dependency.getArtifactId() + "-" + dependency.getVersion()
						+ (StringUtils.isEmpty(dependency.getClassifier()) ? "" : "-" + dependency.getClassifier()) + ".jar");
			}

			getLogger().debug(removeFileNames.size() + " files will be removed.");

			return removeFileNames;

		} catch (Exception e) {
			getLogger().warn("Finding dependent libraries to be removed is skipped. The reason is a '" + e.getMessage() + "'.");
			return new ArrayList<String>();
		}
	}

	/**
	 * Updates .classpath file
	 * 
	 * @param installedPluginJars
	 *            installed plugin jar files
	 * @param baseDir
	 *            the path of current project
	 * @throws Exception
	 */
	public void processClasspathFile(Map<String, File> installedPluginJars, String baseDir) {
		getLogger().debug("Call processClasspathFile() of DefaultPluginUninstaller");

		File classpathFile = new File(baseDir, ".classpath");
		if (!classpathFile.exists()) {
			getLogger().warn("'" + classpathFile.getAbsolutePath() + "' file is not found. Please check a location of your project.");

			return;
		}

		StringBuffer dependencies = new StringBuffer();
		List<String> classentries = new ArrayList<String>();

		try {
			for (File pluginJar : installedPluginJars.values()) {

				List<Dependency> dependencyList = pluginPomManager.getCompileScopeDependencies(pluginJar);

				for (Dependency dependencyInfo : dependencyList) {
					if (classentries.contains(dependencyInfo.getGroupId() + "," + dependencyInfo.getArtifactId() + "," + dependencyInfo.getVersion())) {
						continue;
					}

					dependencies.append("<classpathentry kind=\"lib\" path=\"" + "lib/" + dependencyInfo.getArtifactId() + "-"
							+ dependencyInfo.getVersion() + ((dependencyInfo.getClassifier() != null) ? "-" + dependencyInfo.getClassifier() : "")
							+ ".jar\"/> \n");

					classentries.add(dependencyInfo.getGroupId() + "," + dependencyInfo.getArtifactId() + "," + dependencyInfo.getVersion());
				}
			}

			FileUtil.replaceFileContent(classpathFile, "<!--Add new classpathentry here-->", "</classpath>",
					"<!--Add new classpathentry here-->\n</classpath>", "<!--Add new classpathentry here-->", "<!--Add new classpathentry here-->\n"
							+ dependencies.toString());

		} catch (Exception e) {
			getLogger().warn("Processing '.classpath' file " + " is skipped. The reason is a '" + e.getMessage() + "'.");
		}
	}

	/**
	 * replace plugin resources and delete files to target folder
	 * 
	 * @param baseDir
	 *            plugin project root folder which has plugin sample codes
	 * @param backupDir
	 *            directory for backup
	 * @param fileNames
	 *            resources in plugin jar file
	 * @param pluginInfo
	 *            plugin detail information
	 * @param pluginZip
	 *            zip file includes plugin binary file
	 * @param projectConfig
	 *            project configuration
	 * 
	 */
	private void processTemplates(String baseDir, File backupDir, List<String> fileNames, PluginInfo pluginInfo, ZipFile pluginZip, ProjectConfig projectConfig)
			throws Exception {
		getLogger().debug("Call processTemplates() of DefaultPluginUnInstaller");
		// 1. get resource list
		List<PluginResource> pluginResources = pluginInfo.getResources();
		for (PluginResource pluginResource : pluginResources) {
			getLogger().debug("Processing resources in [" + CommonConstants.METAINF_ANYFRAME + CommonConstants.PLUGIN_FILE + "]");
			// 2. get file list from current resource
			// 2.1 set include
			List<Include> includeResources = pluginResource.getIncludes();
			List<String> includes = new ArrayList<String>();
			for (Include include : includeResources) {
				includes.add(include.getName());
			}

			// 2.2 set exclude
			List<Exclude> excludeResources = pluginResource.getExcludes();
			List<String> excludes = new ArrayList<String>();
			List<String> replaces = new ArrayList<String>();
			for (Exclude exclude : excludeResources) {
				excludes.add(exclude.getName());
				if (exclude.isMerged()) {
					replaces.add(exclude.getName());
				}

			}
			// 2.3 scan resources
			List<String> templates = FileUtil.findFiles(fileNames,
					CommonConstants.PLUGIN_RESOURCES + CommonConstants.fileSeparator + pluginResource.getDir(), includes, excludes);

			String packageName = projectConfig.getPackageName();

			// 4. delete files to base directory
			processTemplate(baseDir, pluginResource.getDir(), backupDir, pluginZip, pluginInfo.getName(), packageName, templates,
					pluginResource.isPackaged());

			getLogger().debug("Remove " + templates.size() + " files");

			if (replaces.size() > 0) {
				List<String> replaceFiles = FileUtil.findFiles(fileNames, CommonConstants.PLUGIN_RESOURCES + CommonConstants.fileSeparator
						+ pluginResource.getDir(), replaces, null);

				// 5. remove contents of files from base directory
				processReplace(pluginInfo.getName(), baseDir, pluginResource.getDir(), packageName, replaceFiles, pluginResource.isPackaged());

				getLogger().debug("Replace " + replaces.size() + " files");
			}

		}
	}

	/**
	 * delete file and folder
	 * 
	 * @param baseDir
	 *            plugin project root folder which has plugin sample codes
	 * @param resourceDir
	 *            plugin resources folder
	 * @param backupDir
	 *            directory for backup
	 * @param pluginZip
	 *            zip file includes plugin binary file
	 * @param packageName
	 *            project's base package name
	 * @param templates
	 *            plugin resources
	 * @param packaged
	 *            whether a plugin resource has package (ex. java)
	 */
	private void processTemplate(String baseDir, String resourceDir, File backupDir, ZipFile pluginZip, String pluginName, String packageName,
			List<String> templates, boolean packaged) throws Exception {

		for (String template : templates) {

			File file = getOutput(baseDir, resourceDir, template, packaged, packageName);

			if (!file.exists())
				return;

			FileUtil.moveFile(baseDir, file, backupDir);

			FileUtil.deleteEmptyDirectory(new File(file.getParent()), true);
		}
	}

	/**
	 * remove contents.
	 * 
	 * @param plguinName
	 *            plugin name
	 * @param baseDir
	 *            plugin project root folder which has plugin sample codes
	 * @param resourceDir
	 *            plugin resources folder
	 * @param packageName
	 *            project's base package name
	 * @param replaceFiles
	 *            merge target resources
	 * @param packaged
	 *            whether a plugin resource has package (ex. java)
	 */
	private void processReplace(String pluginName, String baseDir, String resourceDir, String packageName, List<String> replaceFiles, boolean packaged)
			throws Exception {
		getLogger().debug("Call processReplace() of DefaultPluginInstaller");

		try {
			for (String replaceFile : replaceFiles) {

				File file = getOutput(baseDir, resourceDir, replaceFile, packaged, packageName);

				boolean isPretty = true;
				// 2. remove configuration for current plugin
				try {
					if (file.getName().endsWith(CommonConstants.EXT_JAVA) || file.getName().endsWith(CommonConstants.EXT_JSP) || file.getName().endsWith(CommonConstants.EXT_PROPERTIES)) {
						isPretty = false;
					}
					FileUtil.removeFileContent(file, pluginName + "-configuration", "", isPretty);

					if (pluginName.equals(CommonConstants.I18N_PLUGIN) && replaceFile.endsWith(CommonConstants.CONFIG_MESSAGE_FILE)) {
						FileUtil.removeFileContent(file, pluginName + "-messagesource", "", isPretty);
					}

				} catch (Exception e) {
					getLogger().warn(
							"Removing configuration about " + pluginName + " plugin from " + file.getName() + " is skipped. The reason is a '"
									+ e.getMessage() + "'.");
				}

			}
		} catch (Exception e) {
			getLogger().warn("Merging a file into current project is skipped. The reason is a '" + e.getMessage() + "'.");
		}

	}

	/**
	 * remove a hyperlink from welcome file
	 * 
	 * @param baseDir
	 *            the path of current project
	 * @param pluginName
	 *            plugin name to be uninstalled
	 */
	public void processWelcomeFile(String baseDir, String pluginName) throws Exception {
		getLogger().debug("Call processWelcomeFile() of DefaultPluginUninstaller");
		File indexFile = new File(baseDir + CommonConstants.SRC_MAIN_WEBAPP, CommonConstants.WELCOME_FILE);
		try {
			FileUtil.removeFileContent(indexFile, pluginName + "-menu", "", false);
		} catch (Exception e) {
			getLogger().warn("Removing a menu from current project is skipped. The reason is a '" + e.getMessage() + "'.");
		}
	}

	/**
	 * backup dependent libraries of current plugin
	 * 
	 * @param baseDir
	 *            the path of current project
	 * @param projectType
	 *            generated project's style ('web' or 'service')
	 * @param pluginName
	 *            plugin name
	 * @param installedPluginJars
	 *            jar files of installed plugins
	 * @param removePluginJar
	 *            jar file of plugin to remove
	 * @param properties
	 *            properties of pom in current project
	 * @param backupDir
	 *            directory for backup
	 * @throws Exception
	 */
	public void processDependencyLibs(String baseDir, String pluginName, Map<String, File> installedPluginJars,
			File removePluginJar, Properties properties, File backupDir) {
		getLogger().debug("Call processDependencyLibs() of DefaultPluginUninstaller");

		// 1. remove generated libraries
		File destination = new File(baseDir, CommonConstants.SRC_MAIN_WEBAPP_LIB);

		if (!destination.exists()) {
			return;
		}

		try {
			List<String> removes = findRemoveDependencies(installedPluginJars, removePluginJar, properties);

			FileUtil.moveFile(destination, baseDir, backupDir, removes, null, false);

		} catch (Exception e) {
			e.printStackTrace();
			getLogger().warn(
					"Deleting dependent libraries about '" + pluginName + "' plugin from " + destination + " is skipped. The reason is a '"
							+ e.getMessage() + "'.");
		}
	}

	/**
	 * copy dependent libraries of plugin to target folder
	 * 
	 * @param request
	 *            information includes maven repository, db settings, etc.
	 * @param projectType
	 *            generated project's style ('web' or 'service')
	 * @param baseDir
	 *            the path of current project
	 * @param pluginJar
	 *            plugin binary file
	 */
	public void addDependencyLibs(ArchetypeGenerationRequest request, String baseDir, File pluginJar) throws Exception {
		getLogger().debug("Call processDependencyLibs() of DefaultPluginInstaller");

		// 1. find a destination for copying dependent libararies
		File destination = new File(baseDir, CommonConstants.SRC_MAIN_WEBAPP_LIB);

		if (!destination.exists()) {
			destination.mkdirs();
		}
		List<String> fileNames = FileUtil.resolveFileNames(pluginJar);

		List<String> pomFiles = FileUtil.findFiles(fileNames, CommonConstants.PLUGIN_RESOURCES, "**\\" + CommonConstants.ARCHETYPE_REMOVE_POM, null);
		// 2. merge dependencies of pom file with current dependencies
		if (pomFiles.size() > 0) {
			try {
				File temporaryPomFile = new File(baseDir, "remove-pom.tmp");
				temporaryPomFile.getParentFile().mkdirs();

				InputStream is = pluginInfoManager.getPluginResource(pomFiles.get(0), pluginJar);

				IOUtil.copy(is, new FileOutputStream(temporaryPomFile));

				Model addModel = pluginPomManager.readPom(temporaryPomFile);
				List<Dependency> dependencies = addModel.getDependencies();

				Set<Artifact> dependencyArtifacts = pluginArtifactManager.downloadArtifact(request, dependencies);

				getLogger().debug("Copy " + dependencyArtifacts.size() + " dependent libraries into " + destination.getAbsolutePath());
				Iterator<Artifact> dependencyItr = dependencyArtifacts.iterator();

				while (dependencyItr.hasNext()) {
					Artifact dependencyArtifact = dependencyItr.next();
					if (dependencyArtifact.getScope() == null || dependencyArtifact.getScope().equals("")
							|| dependencyArtifact.getScope().equals("compile")) {

						FileUtil.copyDir(dependencyArtifact.getFile().getCanonicalFile(), destination);
					}
				}

				FileUtil.deleteFile(temporaryPomFile);
			} catch (Exception e) {
				getLogger().debug("Processing a remove-pom.xml of current project is skipped. The reason is a '" + e.getMessage() + "'.");
			}
		} else {
			getLogger().debug(
					"Removing current pom file with that of plugin is skipped. The reason is a remove-pom.xml in " + pluginJar.getName()
							+ " doesn't exist.");
		}
	}

	/**
	 * in case of installing hibernate plugin, change transaction configuration
	 * of foundation
	 * 
	 * @param baseDir
	 *            the path of current project
	 * @param pluginName
	 *            plugin name to be uninstalled
	 */
	public void processTransactionFile(String baseDir) throws Exception {
		getLogger().debug("Call processTransactionFile() of DefaultPluginUninstaller");

		// 1. get a context-transaction.xml file
		File txFile = new File(baseDir + CommonConstants.SRC_MAIN_RESOURCES + "spring", CommonConstants.CONFIG_TX_FILE);

		try {
			FileUtil.replaceFileContent(txFile, "id=\"txManager\" class=\"org.springframework.orm.hibernate3.HibernateTransactionManager\"",
					"id=\"txManager\" class=\"org.springframework.jdbc.datasource.DataSourceTransactionManager\"");
			FileUtil.replaceFileContent(txFile, "<property name=\"sessionFactory\" ref=\"sessionFactory\" />",
					"<property name=\"dataSource\" ref=\"dataSource\" />");
		} catch (Exception e) {
			getLogger().warn(
					"Replace transaction configuration about hibernate" + " plugin from context-transaction.xml is skipped. The reason is a '"
							+ e.getMessage() + "'.");
		}
	}

	/**
	 * in case of uninstalling i18n plugin, change messageSource configuration
	 * 
	 * @param targetDir
	 *            target project folder to install a plugin
	 */
	private void processMessageFile(String baseDir) throws Exception {
		getLogger().debug("Call processMessageFile() of DefaultPluginUnInstaller");
		try {

			// 1. get a transaction configuration file
			File file = new File(baseDir + CommonConstants.SRC_MAIN_RESOURCES + "spring", CommonConstants.CONFIG_MESSAGE_FILE);

			FileUtil.replaceFileContent(file, "<bean id=\"fileMessageSource\"", "<bean id=\"messageSource\"");
		} catch (Exception e) {
			getLogger().warn("Processing a context-message.xml of current project is skipped. The reason is a '" + e.getMessage() + "'.");
		}
	}

	/**
	 * drop custom table, delete data to DB
	 * 
	 * @param baseDir
	 *            the path of current project
	 * @param fileNameList
	 *            file names of plugin jar file
	 * @param pluginName
	 *            plugin name to be uninstalled
	 * @param pluginZip
	 *            plugin jar file
	 * @param encoding
	 *            file encoding style
	 * @param projectConfig
	 *            project configuration
	 */
	public void processInitialData(File baseDir, List<String> fileNameList, String pluginName, ZipFile pluginZip, String encoding, ProjectConfig projectConfig)
			throws Exception {
		getLogger().debug("Call processInitialData() of DefaultPluginUninstaller");

		JdbcOption jdbc = ConfigXmlUtil.getDefaultDatabase(projectConfig);
		String dbType = jdbc.getDbType();

		List<String> dbScripts = FileUtil.findFiles(fileNameList, CommonConstants.PLUGIN_RESOURCES, "**\\" + pluginName + "-delete-data-" + dbType
				+ ".sql", null);
		if (dbScripts.size() > 0) {
			try {
				DBUtil.runStatements(baseDir, pluginName, pluginZip, dbScripts, encoding, jdbc);
				getLogger().debug("Run " + dbScripts + " dbscripts of plugin '" + pluginName + "' successfully.");
			} catch (Exception e) {
				if (e.getCause() instanceof SQLException) {
					getLogger().warn("Executing db script of '" + pluginName + "' plugin is skipped. The reason is " + e.getMessage());
				} else {
					getLogger().warn("Processing initial data for '" + pluginName + "' is skipped. The reason is a '" + e.getMessage() + "'.");
				}
			}
		}
	}

	/**
	 * update installation information of plugin-installed.xml file
	 * 
	 * @param baseDir
	 *            the path of current project
	 * @param pluginName
	 *            plugin name to be installed
	 */
	@SuppressWarnings("unchecked")
	public void updateInstallationInfo(String baseDir, String pluginName) throws Exception {
		getLogger().debug("Call processPluginInstalledFile() of DefaultPluginUninstaller");
		File pluginInstalledFile = new File(baseDir + CommonConstants.METAINF, CommonConstants.PLUGIN_INSTALLED_FILE);
		Map<String, PluginInfo> pluginMap = (Map<String, PluginInfo>) FileUtil.getObjectFromXML(pluginInstalledFile);

		pluginMap.remove(pluginName);

		FileUtil.getObjectToXML(pluginMap, pluginInstalledFile);
	}

	/**
	 * Makes directory using current time.
	 * 
	 * @param baseDir
	 *            the path of current project
	 * @return File object. It's a directory for backup.
	 * @throws Exception
	 */
	private File makeBackupDirectory(String baseDir) {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

		String backupDirName = formatter.format(date);

		File file = new File(baseDir + CommonConstants.UNINSTALLED_FOLDER, backupDirName);

		return file;
	}

	/**
	 * make a output file
	 * 
	 * @param baseDir
	 *            plugin project root folder which has plugin sample codes
	 * @param resourceDir
	 *            resource folder to build plugin resources
	 * @param template
	 *            a plugin resource
	 * @param packaged
	 *            whether a plugin resource has package (ex. java)
	 * @param packageName
	 *            project's base package name
	 * @return output file
	 */
	private File getOutput(String baseDir, String resourceDir, String template, boolean packaged, String packageName) {
		getLogger().debug("Call getOutput() of DefaultPluginPackager");

		template = StringUtils.replaceOnce(template, CommonConstants.PLUGIN_RESOURCES + "/" + resourceDir, "");

		String outputName = resourceDir + CommonConstants.fileSeparator + (packaged ? FileUtil.changePackageForDir(packageName) : "")
				+ CommonConstants.fileSeparator + template;

		File output = new File(baseDir, outputName);

		return output;
	}
}
