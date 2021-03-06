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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.anyframe.ide.command.common.plugin.PluginInfo;
import org.anyframe.ide.command.common.plugin.PluginResource;
import org.anyframe.ide.command.common.util.CommonConstants;
import org.anyframe.ide.command.common.util.ConfigXmlUtil;
import org.anyframe.ide.command.common.util.DBUtil;
import org.anyframe.ide.command.common.util.FileUtil;
import org.anyframe.ide.command.common.util.JdbcOption;
import org.anyframe.ide.command.common.util.ProjectConfig;
import org.anyframe.ide.command.common.util.PropertiesIO;
import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.common.Constants;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

/**
 * This is an DefaultPluginDBChanger class. This class is for changing all db
 * configurations based on selected db
 * 
 * @plexus.component 
 *                   role="org.anyframe.ide.command.common.DefaultPluginDBChanger"
 * @author Sooyeon Park
 */
public class DefaultPluginDBChanger extends AbstractLogEnabled implements PluginDBChanger {
	String ROLE = DefaultPluginDBChanger.class.getName();

	/**
	 * @plexus.requirement 
	 *                     role="org.anyframe.ide.command.common.DefaultPluginArtifactManager"
	 */
	private DefaultPluginArtifactManager pluginArtifactManager;

	/**
	 * @plexus.requirement 
	 *                     role="org.anyframe.ide.command.common.DefaultPluginPomManager"
	 */
	private DefaultPluginPomManager pluginPomManager;

	/**
	 * @plexus.requirement 
	 *                     role="org.anyframe.ide.command.common.DefaultPluginInfoManager"
	 */
	PluginInfoManager pluginInfoManager;

	/**
	 * change db configuration and data
	 * 
	 * @param request
	 *            information includes maven repository, db settings, plugin
	 * @param baseDir
	 *            current project folder
	 * @param encoding
	 *            file encoding style
	 */
	public void change(ArchetypeGenerationRequest request, String baseDir, String encoding) throws Exception {
		getLogger().debug(DefaultPluginDBChanger.class.getName() + " execution start.");
		try {
			String configFile = ConfigXmlUtil.getCommonConfigFile(baseDir);
			ProjectConfig projectConfig = ConfigXmlUtil.getProjectConfig(configFile);

			checkProject(baseDir);

			JdbcOption jdbcOption = ConfigXmlUtil.getDefaultDatabase(projectConfig);

			String dbType = jdbcOption.getDbType();

			String anyframeHome = projectConfig.getAnyframeHome();
			String buildType = CommonConstants.PROJECT_BUILD_TYPE_MAVEN;
			if (anyframeHome != null && !"".equals(anyframeHome)) {
				buildType = CommonConstants.PROJECT_BUILD_TYPE_ANT;
			}

			String templateHome = projectConfig.getTemplatePath(CommonConstants.PROJECT_NAME_CODE_GENERATOR);

			// 1. process hibernate.cfg.xml of current project
			//processHibernateCfgFile(jdbcOption, baseDir);

			// 2. process hibernate.reveng.ftl
			//processHibernateReverseEngTemplate(jdbcOption, templateHome);

			// 3. process context.properties
			processDBProperties(jdbcOption, baseDir);

			// 4. process plugin xml files related to db properties and db
			// scripts
			Map<String, PluginInfo> installedPluginMap = pluginInfoManager.getInstalledPlugins(baseDir);

			Set<String> installedPluginSet = installedPluginMap.keySet();
			Iterator<String> instlledPluginItr = installedPluginSet.iterator();

			while (instlledPluginItr.hasNext()) {
				String pluginName = instlledPluginItr.next();
				PluginInfo pluginInfo = installedPluginMap.get(pluginName);

				File pluginJar = pluginInfoManager.getPluginFile(request, pluginInfo.getGroupId(), pluginInfo.getArtifactId(),
						pluginInfo.getVersion());

				ZipFile pluginZip = pluginArtifactManager.getArchetypeZipFile(pluginJar);

				// 4.1 process plugin xml files related to db properties
				processDbResources(projectConfig, baseDir, pluginName, pluginJar, pluginZip, dbType);

				// 4.2 create custom table, insert data to DB
				processInitialData(jdbcOption, baseDir, pluginName, pluginJar, pluginZip, dbType, encoding);
			}

			// 5. copy jdbc jar file and add classpath information
			processDBLibs(projectConfig, jdbcOption, buildType, dbType, baseDir);

			// 6. process project database configuration
			// processMetadata(jdbcOption, baseDir, buildType);

			getLogger().info("Current project is changed to be appropriate for '" + dbType + "' successfully.");
		} catch (Exception e) {
			if (e instanceof CommandException)
				throw e;
			throw new CommandException("Error occurred in changing db. The reason is a '" + e.getMessage() + "'.");
		}
	}

	/**
	 * check a project status whether change-db is possible
	 * 
	 * @param baseDir
	 *            current project folder
	 */
	private void checkProject(String baseDir) throws Exception {
		getLogger().debug("Call checkProject() of DefaultPluginDBChanger");
		Map<String, PluginInfo> installedPlugins = pluginInfoManager.getInstalledPlugins(baseDir);

		if (installedPlugins.size() == 0) {
			throw new CommandException("Can not find any installed plugin information. Please install any plugin at the very first.");
		}

		if (!installedPlugins.containsKey(CommonConstants.CORE_PLUGIN)) {
			throw new CommandException("Can not find installed plugin ['core'] information. Please install 'core' plugin at the very first.");
		}
	}

	/**
	 * change hibernate.cfg.xml file
	 * 
	 * @param jdbcOption
	 *            project database configuration
	 * @param baseDir
	 *            current project folder
	 */
	private void processHibernateCfgFile(JdbcOption jdbcOption, String baseDir) throws Exception {
		getLogger().debug("Call processHibernateCfgFile() of DefaultPluginDBChanger");

		File hibernateCfgFile = new File(baseDir + CommonConstants.SRC_MAIN_RESOURCES + "hibernate" + CommonConstants.fileSeparator
				+ "hibernate.cfg.xml");

		if (!hibernateCfgFile.exists()) {
			getLogger().warn("'" + hibernateCfgFile.getAbsolutePath() + "' file is not found. Please check a location of your project.");
			return;
		}

		String replaceString = "";

		try {
			String metadataDialect = "";
			if (jdbcOption.getDbType().equalsIgnoreCase("sybase")) {
				metadataDialect = "<property name=\"hibernatetool.metadatadialect\">org.anyframe.ide.command.maven.mojo.codegen.dialect.SybaseMetaDataDialect</property>\n";
			}

			FileUtil.removeFileContent(hibernateCfgFile, "hibernate jdbc configuration", "", true);

			replaceString = "<!--hibernate jdbc configuration-START-->\n" + "<property name=\"hibernate.dialect\">" + jdbcOption.getDialect()
					+ "</property>\n" + metadataDialect + "<property name=\"hibernate.connection.driver_class\">" + jdbcOption.getDriverClassName()
					+ "</property>\n" + "<property name=\"hibernate.connection.username\">" + jdbcOption.getUserName() + "</property>\n"
					+ "<property name=\"hibernate.connection.password\">" + jdbcOption.getPassword() + "</property>\n"
					+ "<property name=\"hibernate.connection.url\">" + jdbcOption.getUrl() + "</property>\n"
					+ "<!--hibernate jdbc configuration-END-->";
			FileUtil.addFileContent(hibernateCfgFile, "<!--hibernate jdbc configuration here-->", "<!--hibernate jdbc configuration here-->\n"
					+ replaceString, true);
		} catch (Exception e) {
			getLogger().warn(
					"Replacing hibernate jdbc configuration in hibernate.cfg.xml of current project is skipped." + ". The reason is a '"
							+ e.getMessage() + "'.");
		}
	}

	/**
	 * change hibernate.reveng.ftl template
	 * 
	 * @param jdbcOption
	 *            project database configuration
	 * @param templateHome
	 *            folder which have templates
	 */
	private void processHibernateReverseEngTemplate(JdbcOption jdbcOption, String templateHome) throws Exception {
		getLogger().debug("Call processHibernateReverseEngTemplate() of DefaultPluginDBChanger");
		// 1. find templates directory
		File templateHomeDir = new File(templateHome);

		if (!templateHomeDir.exists()) {
			getLogger().warn("'" + templateHomeDir.getAbsolutePath() + "' folder is not found. Please check a location of template home directory.");

			return;
		}

		// 2. find hibernate.reveng.ftl file
		File hibernateReverseEngTemplateFile = new File(templateHomeDir.getAbsolutePath(), "default" + CommonConstants.fileSeparator + "source"
				+ CommonConstants.fileSeparator + "model" + CommonConstants.fileSeparator + "hibernate.reveng.ftl");

		if (!hibernateReverseEngTemplateFile.exists()) {
			getLogger().warn(
					"'" + hibernateReverseEngTemplateFile.getAbsolutePath()
							+ "' is not found. Please check a location of hibernate.reveng.ftl in default template directory(default/source/model).");

			return;
		}

		String replaceString = null;

		try {
			// 3. replace
			if (jdbcOption.getDbType().equalsIgnoreCase("sybase")) {
				replaceString = "<!--schema-selection-START-->\n" + "<!--schema-selection-END-->";

				FileUtil.replaceStringXMLFilePretty(hibernateReverseEngTemplateFile, "schema-selection", "<!--schema-selection here-->",
						"<!--schema-selection here-->\n" + replaceString);
			} else {
				replaceString = "<!--schema-selection-START-->\n" + "<schema-selection match-schema=\"${schema}\"/>" + "<!--schema-selection-END-->";

				FileUtil.replaceStringXMLFilePretty(hibernateReverseEngTemplateFile, "schema-selection", "<!--schema-selection here-->",
						"<!--schema-selection here-->\n" + replaceString);
			}
		} catch (Exception e) {
			getLogger().warn(
					"Replacing schema-selection in " + hibernateReverseEngTemplateFile.getAbsolutePath() + " is skipped. The reason is a '"
							+ e.getMessage() + "'.");
		}
	}

	/**
	 * change context.properties file
	 * 
	 * @param jdbcOption
	 *            project database configuration
	 * @param baseDir
	 *            current project folder
	 */
	private void processDBProperties(JdbcOption jdbcOption, String baseDir) throws Exception {
		getLogger().debug("Call processDBProperties() of DefaultPluginDBChanger");

		File dbPropertiesFile = null;
		try {
			dbPropertiesFile = new File(baseDir, CommonConstants.SRC_MAIN_RESOURCES + CommonConstants.CONTEXT_PROPERTIES);

			if (!dbPropertiesFile.exists()) {
				getLogger().warn("'" + dbPropertiesFile.getAbsolutePath() + "' is not found. Please check a location of your project.");

				return;
			}

			PropertiesIO contextPio = new PropertiesIO(baseDir + CommonConstants.SRC_MAIN_RESOURCES + CommonConstants.CONTEXT_PROPERTIES);

			contextPio.setProperty(CommonConstants.APP_DB_DRIVER_CLASS, jdbcOption.getDriverClassName());
			contextPio.setProperty(CommonConstants.APP_DB_URL, jdbcOption.getUrl());
			contextPio.setProperty(CommonConstants.APP_DB_USERNAME, jdbcOption.getUserName());
			contextPio.setProperty(CommonConstants.APP_DB_PASSWORD, jdbcOption.getPassword());

			contextPio.write();
		} catch (Exception e) {
			getLogger().warn(
					"Replacing db properties in " + dbPropertiesFile.getAbsolutePath() + " is skipped. The reason is a '" + e.getMessage() + "'.");
		}
	}

	/**
	 * change all xml files (ex. spring xml, mapping xml, ...)
	 * 
	 * @param projectConfig
	 *            project configuration
	 * @param baseDir
	 *            current project folder
	 * @param pluginName
	 *            plugin's name which have resources to be changed
	 * @param pluginJar
	 *            plugin jar file
	 * @param pluginZip
	 *            plugin zip file
	 * 
	 * @param dbType
	 *            db type
	 */
	private void processDbResources(ProjectConfig projectConfig, String baseDir, String pluginName, File pluginJar, ZipFile pluginZip, String dbType)
			throws Exception {
		getLogger().debug("Call processXMLFile() of DefaultPluginDBChanger, pluginName is " + pluginName);

		try {
			// 1. find db resource templates
			String path = CommonConstants.DB_RESOURCES + CommonConstants.fileSeparator + dbType;

			List<String> dbResources = findPluginResources(pluginJar, path, "**");

			getLogger().debug("dbResources size : " + dbResources.size());

			List<PluginResource> pluginResources = pluginInfoManager.getPluginInfo(pluginJar).getResources();

			for (int i = 0; i < dbResources.size(); i++) {
				String dbResourceTemplate = (String) dbResources.get(i);

				// 2. find real file(xml, java) equals to db resource template
				for (int j = 0; j < pluginResources.size(); j++) {
					PluginResource pluginResource = pluginResources.get(j);

					String resourceDir = pluginResource.getDir();
					String packageName = projectConfig.getPackageName().trim();

					String dbResourceTemplateName = StringUtils.replaceOnce(dbResourceTemplate, CommonConstants.DB_RESOURCES + dbType + "/"
							+ resourceDir, "");

					File dbResourceFile = new File(baseDir, resourceDir + CommonConstants.fileSeparator
							+ (pluginResource.isPackaged() ? FileUtil.changePackageForDir(packageName) : "") + CommonConstants.fileSeparator
							+ dbResourceTemplateName);

					if (dbResourceFile.exists()) {
						getLogger().debug("dbResourceTemplate : " + dbResourceTemplate);
						getLogger().debug("dbResourceFile : " + dbResourceFile);

						// 3. replace
						replaceDBResource(pluginName, pluginZip, dbResourceFile, dbResourceTemplate);

						getLogger().debug("Change " + dbResourceFile.getAbsolutePath() + " file of current project successfully.");
					}
				}
			}
		} catch (Exception e) {
			getLogger().warn("Replacing db configuration in xml file is skipped." + ". The reason is a '" + e.getMessage() + "'.");
		}
	}

	/**
	 * create custom table, insert data to DB
	 * 
	 * @param jdbcOption
	 *            project database configuration
	 * @param baseDir
	 *            target folder
	 * @param pluginName
	 *            plugin name to be installed
	 * @param pluginJar
	 *            plugin jar file
	 * @param pluginZip
	 *            plugin zip file
	 * @param dbType
	 *            db type
	 * @param encoding
	 *            file encoding style
	 * 
	 */
	public void processInitialData(JdbcOption jdbcOption, String baseDir, String pluginName, File pluginJar, ZipFile pluginZip, String dbType,
			String encoding) throws Exception {
		getLogger().debug("Call processInitialData() of DefaultPluginDBChanger");
		List<String> dbScripts = findPluginResources(pluginJar, CommonConstants.PLUGIN_RESOURCES, "**\\" + pluginName + "-insert-data-" + dbType
				+ ".sql");

		if (dbScripts.size() > 0) {
			try {
				DBUtil.runStatements(new File(baseDir), pluginName, pluginZip, dbScripts, encoding, jdbcOption);
				getLogger().debug("Run " + dbScripts + " dbscripts of plugin [" + pluginName + "] successfully.");
			} catch (Exception e) {
				if (e.getCause() instanceof SQLException) {
					getLogger().warn("Executing db script of " + pluginName + " plugin is skipped. The reason is '" + e.getMessage() + "'.");
				} else {
					getLogger().warn("Processing initial data for " + pluginName + " is skipped. The reason is a '" + e.getMessage() + "'.");
				}
			}
		}
	}

	/**
	 * find plugin resources from plugin jar file
	 * 
	 * @param pluginJar
	 *            plugin jar file
	 * @param path
	 *            path for finding plugin's resources
	 * @param pattern
	 *            pattern for file name
	 * @return plugin resources
	 */
	private List<String> findPluginResources(File pluginJar, String path, String pattern) throws Exception {
		List<String> fileNames = FileUtil.resolveFileNames(pluginJar);
		return FileUtil.findFiles(fileNames, path, pattern, null);
	}

	/**
	 * replace current configuration file based on db resource template
	 * 
	 * @param pluginName
	 *            plugin's name which have resources to be changed
	 * @param pluginZip
	 *            plugin zip file
	 * @param dbResource
	 *            current configuration file related to db
	 * @param dbResourceTemplate
	 *            plugin resource related to db
	 */
	private void replaceDBResource(String pluginName, ZipFile pluginZip, File dbResource, String dbResourceTemplate) throws Exception {
		// if (dbResource.exists()) {
		// 1. find map includes replace string
		ZipEntry zipEntry = pluginZip.getEntry(dbResourceTemplate);
		InputStream templateInputStream = pluginZip.getInputStream(zipEntry);

		Map<String, String> replaceStringMap = null;
		Map<String, String> tokenMap = null;

		if (dbResource.getName().endsWith(CommonConstants.EXT_JAVA)) {
			replaceStringMap = FileUtil.findReplaceRegionOfClass(templateInputStream, pluginName);
			tokenMap = FileUtil.findReplaceRegionOfClass(new FileInputStream(dbResource), pluginName);
		}

		if (dbResource.getName().endsWith(CommonConstants.EXT_XML)) {
			replaceStringMap = FileUtil.findReplaceRegion(templateInputStream, pluginName);

			// 2. find token to be replaced
			tokenMap = FileUtil.findReplaceRegion(new FileInputStream(dbResource), pluginName);
		}

		getLogger().debug("token size : " + tokenMap.size());
		getLogger().debug("replaceString size : " + replaceStringMap.size());

		// 3. replace
		if (tokenMap.size() > 0) {
			Set<String> commentKeySet = tokenMap.keySet();
			Iterator<String> commentKeyItr = commentKeySet.iterator();

			while (commentKeyItr.hasNext()) {
				String commentKey = commentKeyItr.next();
				getLogger().debug("commentKey : " + commentKey);

				if (replaceStringMap.containsKey(commentKey)) {

					String startToken = "<!--" + pluginName + "-" + commentKey + "-START-->";
					String endToken = "<!--" + pluginName + "-" + commentKey + "-END-->";

					if (dbResource.getName().endsWith(CommonConstants.EXT_JAVA)) {
						startToken = "//" + pluginName + "-" + commentKey + "-START";
						endToken = "//" + pluginName + "-" + commentKey + "-END";
					}

					String value = startToken + "\n" + replaceStringMap.get(commentKey) + "\n" + endToken;
					FileUtil.replaceFileContent(dbResource, startToken, endToken, startToken + endToken, value, false);
				}
			}
		}
		// }
	}

	/**
	 * change .classpath file and process library. if current project is based
	 * on maven, change pom.xml file. if current project is based on ant, copy
	 * library to specific target
	 * 
	 * @param jdbcOption
	 *            project database configuration
	 * @param buildType
	 *            build type of project ('maven' or 'ant')
	 * @param dbType
	 *            db type (hsqldb, oracle, sybase, ...)
	 * @param baseDir
	 *            current project folder
	 */
	private void processDBLibs(ProjectConfig projectConfig, JdbcOption jdbcOption, String buildType, String dbType, String baseDir) throws Exception {
		// 1. maven --> pom.xml, .classpath (maven)
		if (buildType.equalsIgnoreCase(CommonConstants.PROJECT_BUILD_TYPE_MAVEN)) {
			String groupId = jdbcOption.getMvnGroupId();
			String artifactId = jdbcOption.getMvnArtifactId();
			String version = jdbcOption.getMvnVersion();
			processPom(baseDir, groupId, artifactId, version);

			return;
		}
		// 2. ant --> .classpath, copy
		String driverPath = jdbcOption.getDriverJar();
		copyDBLibs(projectConfig, jdbcOption, baseDir, driverPath);
	}

	/**
	 * @param jdbcOption
	 *            project database configuration
	 * @param buildType
	 *            build type of project ('maven' or 'ant')
	 */
	private void processMetadata(JdbcOption jdbcOption, String baseDir, String buildType) throws Exception {
		if (buildType.equalsIgnoreCase(CommonConstants.PROJECT_BUILD_TYPE_ANT)) {
			jdbcOption.setMvnGroupId("");
			jdbcOption.setMvnArtifactId("");
			jdbcOption.setMvnVersion("");
		}

		// in case of sybase
		if (jdbcOption.getDbType().equalsIgnoreCase("sybase")) {
			jdbcOption.setSchema("");
		}
		
		ConfigXmlUtil.saveDatabase(baseDir, jdbcOption);
	}

	/**
	 * change pom.xml file
	 * 
	 * @param baseDir
	 *            current project folder
	 * @param groupId
	 *            groupId of db library
	 * @param artifactId
	 *            artifactId of db library
	 * @param version
	 *            version of db library
	 */
	@SuppressWarnings("unchecked")
	private void processPom(String baseDir, String groupId, String artifactId, String version) throws Exception {
		// 1. get pom file
		File pomFile = new File(baseDir, Constants.ARCHETYPE_POM);

		// 2. add dependencies of pom file
		if (pomFile.exists()) {
			try {
				Model model = pluginPomManager.readPom(pomFile);

				// 2.1 process dependencies
				List<Dependency> dependencies = model.getDependencies();

				boolean isDefined = false;
				for (int i = 0; i < dependencies.size(); i++) {
					Dependency dependency = dependencies.get(i);
					if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)
							&& dependency.getVersion().equals(version)) {
						isDefined = true;
						break;
					}
				}

				Dependency dependency = new Dependency();
				dependency.setGroupId(groupId);
				dependency.setArtifactId(artifactId);
				dependency.setVersion(version);

				if (!isDefined) {
					model.addDependency(dependency);
				}

				// 2.2 process build-plugin dependencies
				Map<String, Plugin> buildPluginMap = model.getBuild().getPluginsAsMap();

				String anyframePlugin = "org.codehaus.mojo:anyframe-maven-plugin";

				if (buildPluginMap.containsKey(anyframePlugin)) {
					Plugin plugin = buildPluginMap.get(anyframePlugin);

					List<Dependency> pluginDependencies = new ArrayList<Dependency>();
					pluginDependencies.add(dependency);
					plugin.setDependencies(pluginDependencies);
				}

				pluginPomManager.writePom(model, pomFile, pomFile);
			} catch (Exception e) {
				getLogger().warn("Processing a pom.xml of current project is skipped. The reason is a '" + e.getMessage() + "'.");
			}
		}
	}

	/**
	 * copy db library to specific folder ('web' type project ->
	 * baseDir/src/main/webapp/WEB-INF/lib, 'service' type project ->
	 * baseDir/lib)
	 * 
	 * @param projectConfig
	 *            project configuration
	 * @param jdbcOption
	 *            project database configuration
	 * @param baseDir
	 *            current project folder
	 * @param driverPath
	 *            path includes db library
	 */
	private void copyDBLibs(ProjectConfig projectConfig, JdbcOption jdbcOption, String baseDir, String driverPath) throws Exception {
		driverPath = driverPath.trim();

		File dbLibFile = new File(driverPath);
		if (!dbLibFile.exists()) {
			dbLibFile = new File(baseDir, driverPath);
		}

		if (!dbLibFile.exists()) {
			return;
		}

		String projectName = projectConfig.getPjtName();
		if (projectName != null && projectName.trim().length() > 0) {
			try {
				String inDestinationDirectory = baseDir + CommonConstants.SRC_MAIN_WEBAPP_LIB;
				FileUtil.copyJars(jdbcOption.getDriverJar(), inDestinationDirectory, false);
			} catch (Exception e) {
				getLogger().warn(
						"Copying jdbc jar file into /src/main/webapp/WEB-INF/lib is skipped. The reason is jdbc jar file is not found in "
								+ driverPath + ".");
			}
		}
	}

	/**
	 * make classpath information (in case of 'service' type project)
	 * 
	 * @param baseDir
	 *            current project folder
	 * @param driverPath
	 *            path includes db library
	 * @return db library path
	 */
	private String getDBDriverPath(String baseDir, String driverPath) throws Exception {
		int idx = driverPath.lastIndexOf("/");
		if (idx == -1) {
			idx = driverPath.lastIndexOf("\\");
		}

		String fileName = "";
		if (idx != -1) {
			fileName = driverPath.substring(idx + 1);
			return baseDir + "/" + "lib" + "/" + fileName;
		}

		return baseDir + "/" + "lib" + "/" + driverPath;
	}

	/**
	 * change .classpath file
	 * 
	 * @param baseDir
	 *            current project folder
	 * @param dbType
	 *            db type (hsqldb, oracle, sybase, ...)
	 * @param dbDriverPath
	 *            path includes db library
	 */
	private void changeClasspath(String baseDir, String dbType, String dbDriverPath) throws Exception {
		try {
			// 1. find .classpath file
			File classpathFile = new File(baseDir, ".classpath");

			// 2. remove previous driver jar path
			FileUtil.removeFileContent(classpathFile, "Driver jar path", "", true);

			if (dbType.equalsIgnoreCase("hsqldb")) {
				// 3. add current driver jar path
				String replaceString = "<!--Driver jar path-START-->\n" + "<!--Driver jar path-END-->";
				FileUtil.addFileContent(classpathFile, "<!--Driver jar path here-->", "<!--Driver jar path here-->\n" + replaceString, true);

				return;
			}

			int idx = dbDriverPath.lastIndexOf("/");
			if (idx == -1) {
				idx = dbDriverPath.lastIndexOf("\\");
			}

			String dbLibFileName = dbDriverPath.substring(idx + 1);

			// 3. add current driver jar path
			String replaceString = "<!--Driver jar path-START-->\n" + "<classpathentry kind=\"lib\" path=\"lib/" + dbLibFileName + "\"/>\n"
					+ "<!--Driver jar path-END-->";
			FileUtil.addFileContent(classpathFile, "<!--Driver jar path here-->", "<!--Driver jar path here-->\n" + replaceString, true);
		} catch (Exception e) {
			// ignore Exception
			catchMsg(e, "Replacing driver jar path", "/.classpath", "<!--Driver jar path here-->", baseDir);
		}
	}

	/**
	 * logging warning message
	 * 
	 * @param e
	 *            exception
	 * @param exceptionMsg
	 *            exception message
	 * @param fileName
	 *            file name which have problem
	 * @param tokenName
	 * @param path
	 */
	public void catchMsg(Exception e, String exceptionMsg, String fileName, String tokenName, String path) {
		if (e instanceof FileNotFoundException)
			getLogger().warn(exceptionMsg + " in " + fileName + " is skipped. The reason is " + fileName + " is not found in " + path + ".");
		else
			getLogger().warn(
					exceptionMsg + " in " + fileName + " is skipped. The reason is " + tokenName + " token is not found in " + path + fileName + ".");

	}
}
