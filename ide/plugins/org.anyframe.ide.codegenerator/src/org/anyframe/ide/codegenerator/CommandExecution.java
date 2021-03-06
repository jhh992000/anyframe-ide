/*   
 * Copyright 2008-2013 the original author or authors.   
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
package org.anyframe.ide.codegenerator;

import org.anyframe.ide.codegenerator.command.factory.AbstractCommandFactory;
import org.anyframe.ide.codegenerator.command.factory.CommandFactory;
import org.anyframe.ide.codegenerator.command.vo.CommandVO;
import org.anyframe.ide.codegenerator.command.vo.CreateCRUDVO;
import org.anyframe.ide.codegenerator.command.vo.CreateModelVO;
import org.anyframe.ide.codegenerator.command.vo.CreatePJTVO;
import org.anyframe.ide.codegenerator.command.vo.InstallPluginVO;
import org.anyframe.ide.codegenerator.command.vo.UninstallPluginVO;
import org.anyframe.ide.codegenerator.messages.Message;
import org.anyframe.ide.codegenerator.wizards.ApplicationData;
import org.anyframe.ide.command.cli.util.CommandUtil;
import org.anyframe.ide.command.common.util.CommonConstants;
import org.anyframe.ide.common.util.ConfigXmlUtil;
import org.anyframe.ide.common.util.MessageDialogUtil;
import org.anyframe.ide.common.util.PluginLoggerUtil;
import org.anyframe.ide.common.util.ProjectConfig;
import org.anyframe.ide.common.util.StringUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * This is a CommandExecution class.
 * 
 * @author Changje Kim
 * @author Sooyeon Park
 */
public class CommandExecution {

	public static final String ID = CodeGeneratorActivator.PLUGIN_ID;

	public CommandExecution() {

	}

	public void createProject(String pjtType, String pjtName, String appPath,
			ApplicationData applicationData, IStructuredSelection selection, String pluginName) {

		// 1. make arguments
		CreatePJTVO vo = new CreatePJTVO();
		vo.setCommand(CommandUtil.CMD_CREATE_PROJECT);
		vo.setProjectName(pjtName);
		vo.setPackageName(applicationData.getAppPackage());
		vo.setProjectGroupId(applicationData.getPjtGroupId());
		vo.setProjectVersion(applicationData.getPjtVersion());
		vo.setProjectType(pjtType);
		vo.setProjectHome(appPath);
		vo.setAnyframeHome(applicationData.getAnyframeHome());
		vo.setBasedir(appPath);
		vo.setDatabaseType(applicationData.getDatabaseType());
		vo.setDatabaseName(applicationData.getDatabaseName());
		vo.setDatabaseDriverPath(applicationData.getDriverJar());
		vo.setSelection(selection);
		vo.setTemplateHome(applicationData.getPjtTemplateHome());
		vo.setInspectionHome(applicationData.getInspectionHome());
		vo.setDatabaseSchema(applicationData.getSchema());
		vo.setDatabaseUserId(applicationData.getUseName());
		vo.setDatabasePassword(applicationData.getPassword());
		vo.setDatabaseServer(applicationData.getServer());
		vo.setDatabasePort(applicationData.getPort());
		vo.setDatabaseUrl(applicationData.getUrl());
		vo.setDatabaseDialect(applicationData.getDialect());
		vo.setDatabaseGroupId(applicationData.getDriverGroupId());
		vo.setDatabaseArtifactId(applicationData.getDriverArtifactId());
		vo.setDatabaseVersion(applicationData.getDriverVersion());
		vo.setDatabaseDriver(applicationData.getDriverClassName());
		vo.setOffline(applicationData.isOffine());
		
		// add vo into plugin name
		vo.setPluginName(pluginName);
		
		// 2. run ant/maven
		String buildType = applicationData.isAntProject() ? CommonConstants.PROJECT_BUILD_TYPE_ANT
				: CommonConstants.PROJECT_BUILD_TYPE_MAVEN;
		AbstractCommandFactory factory = new CommandFactory(buildType);
		factory.newInstance().execute(vo);
	}

	public void createCRUD(String domainClassName, String srcPackage,
			String serviceProjectName, String templateType, boolean createWebProject,
			boolean insertSampleData, String projectLocation) {

		// 1. get application location
		String anyframeHomeLocation = "";
		String projectBuildType = "";
		try {
			String configFile = ConfigXmlUtil.getCommonConfigFile(projectLocation);
			ProjectConfig projectConfig = ConfigXmlUtil.getProjectConfig(configFile);
			anyframeHomeLocation = projectConfig.getAnyframeHome();

			if(StringUtil.isEmptyOrNull(projectConfig.getAnyframeHome())){
				projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_MAVEN;
			}else{
				projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_ANT;
			}
		} catch (Exception e) {
			PluginLoggerUtil.error(ID, Message.view_exception_loadconfig, e);
			MessageDialogUtil.openMessageDialog(Message.ide_message_title,
					Message.view_exception_findconfig, MessageDialog.ERROR);
		}

		String scope = CommonConstants.PROJECT_TYPE_SERVICE;
		if (createWebProject)
			scope = "all";
		String sampleData = "false";
		if (insertSampleData)
			sampleData = "true";

		// 2. make arguments
		CreateCRUDVO vo = new CreateCRUDVO();
		vo.setCommand(CommandUtil.CMD_CREATE_CRUD);
		vo.setDomainClassName(domainClassName);
		vo.setProjectName(serviceProjectName);
		vo.setPackageName(srcPackage);
		vo.setScope(scope);
		vo.setProjectHome(projectLocation);
		vo.setAnyframeHome(anyframeHomeLocation);
		vo.setBasedir(projectLocation);
		vo.setInsertSampleData(sampleData);
		vo.setTemplateType(templateType);

		// 3. run ant/maven
		AbstractCommandFactory factory = new CommandFactory(projectBuildType);
		factory.newInstance().execute(vo);
	}

	/**
	 * Build configuration of mapping
	 */
	public void createModel(String tableName, String srcPackage,
			String projectLocation) throws Exception {
		// 1. get project location, package name
		String projectBuildType = "";
		String projectName = "";
		String anyframeHomeLocation = "";
		try {
			String configFile = ConfigXmlUtil.getCommonConfigFile(projectLocation);
			ProjectConfig projectConfig = ConfigXmlUtil.getProjectConfig(configFile);
			projectName = projectConfig.getPjtName();
			anyframeHomeLocation = projectConfig.getAnyframeHome();

			if(StringUtil.isEmptyOrNull(anyframeHomeLocation)){
				projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_MAVEN;
			}else{
				projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_ANT;
			}
		} catch (Exception e) {
			PluginLoggerUtil.error(ID, Message.view_exception_loadconfig, e);
			MessageDialogUtil.openMessageDialog(Message.ide_message_title,
					Message.view_exception_findconfig, MessageDialog.ERROR);
		}

		// 2. make arguments
		CreateModelVO vo = new CreateModelVO();
		vo.setCommand(CommandUtil.CMD_CREATE_MODEL);
		vo.setTableName(tableName);
		vo.setPackageName(srcPackage);
		vo.setProjectHome(projectLocation);
		vo.setAnyframeHome(anyframeHomeLocation);
		vo.setBasedir(projectLocation);
		vo.setProjectName(projectName);

		// 3. run ant
		AbstractCommandFactory factory = new CommandFactory(projectBuildType);
		factory.newInstance().execute(vo);
	}

	public void changeDBConfig(String projectLocation) {

		// 1. get project location
		String anyframeHomeLocation = "";
		String projectBuildType = "";
		String projectName = "";
		try {
			String configFile = ConfigXmlUtil.getCommonConfigFile(projectLocation);
			ProjectConfig projectConfig = ConfigXmlUtil.getProjectConfig(configFile);
			anyframeHomeLocation = projectConfig.getAnyframeHome();
			projectName = projectConfig.getPjtName();

			if(StringUtil.isEmptyOrNull(projectConfig.getAnyframeHome())){
				projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_MAVEN;
			}else{
				projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_ANT;
			}
		} catch (Exception e) {
			PluginLoggerUtil.error(ID, Message.view_exception_loadconfig, e);
			MessageDialogUtil.openMessageDialog(Message.ide_message_title,
					Message.view_exception_findconfig, MessageDialog.ERROR);
		}

		// 2. make arguments
		CommandVO vo = new CommandVO();
		vo.setCommand(CommandUtil.CMD_CHANGE_DB);
		vo.setProjectHome(projectLocation);
		vo.setAnyframeHome(anyframeHomeLocation);
		vo.setBasedir(projectLocation);
		vo.setProjectName(projectName);

		// 3. run ant
		AbstractCommandFactory factory = new CommandFactory(projectBuildType);
		factory.newInstance().execute(vo);
	}

	public void installPlugins(String pluginNames, boolean installSample,
			String projectLocation) {
		try {
			// 1. get project location
			String anyframeHomeLocation = "";
			String projectBuildType = "";
			String projectName = "";
			try {
				String configFile = ConfigXmlUtil.getCommonConfigFile(projectLocation);
				ProjectConfig projectConfig = ConfigXmlUtil.getProjectConfig(configFile);
				anyframeHomeLocation = projectConfig.getAnyframeHome();
				projectName = projectConfig.getPjtName();

				if(StringUtil.isEmptyOrNull(projectConfig.getAnyframeHome())){
					projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_MAVEN;
				}else{
					projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_ANT;
				}
			} catch (Exception e) {
				PluginLoggerUtil
						.error(ID, Message.view_exception_loadconfig, e);
				MessageDialogUtil.openMessageDialog(Message.ide_message_title,
						Message.view_exception_findconfig, MessageDialog.ERROR);
			}

			// 2. make arguments
			InstallPluginVO vo = new InstallPluginVO();
			vo.setCommand(CommandUtil.CMD_INSTALL);
			vo.setProjectHome(projectLocation);
			vo.setAnyframeHome(anyframeHomeLocation);
			vo.setBasedir(projectLocation);
			vo.setPluginNames(pluginNames);
			vo.setExcludeSrc(!installSample);
			vo.setProjectName(projectName);

			// 3. run ant
			AbstractCommandFactory factory = new CommandFactory(
					projectBuildType);
			factory.newInstance().execute(vo);

		} catch (Exception e) {
			PluginLoggerUtil.error(ID,
					Message.view_exception_installpluginlist, e);
		}
	}

	public void uninstallPlugins(String pluginNames, boolean uninstallSample,
			String projectLocation) {
		try {
			// 1. get project location
			String anyframeHomeLocation = "";
			String projectBuildType = "";
			String projectName = "";
			try {
				String configFile = ConfigXmlUtil.getCommonConfigFile(projectLocation);
				ProjectConfig projectConfig = ConfigXmlUtil.getProjectConfig(configFile);
				anyframeHomeLocation = projectConfig.getAnyframeHome();
				projectName = projectConfig.getPjtName();

				if(StringUtil.isEmptyOrNull(projectConfig.getAnyframeHome())){
					projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_MAVEN;
				}else{
					projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_ANT;
				}
			} catch (Exception e) {
				PluginLoggerUtil
						.error(ID, Message.view_exception_loadconfig, e);
				MessageDialogUtil.openMessageDialog(Message.ide_message_title,
						Message.view_exception_findconfig, MessageDialog.ERROR);
			}

			// 2. make arguments
			UninstallPluginVO vo = new UninstallPluginVO();
			vo.setCommand(CommandUtil.CMD_UNINSTALL);
			vo.setProjectHome(projectLocation);
			vo.setAnyframeHome(anyframeHomeLocation);
			vo.setBasedir(projectLocation);
			vo.setPluginNames(pluginNames);
			vo.setProjectName(projectName);

			// 3. run ant
			AbstractCommandFactory factory = new CommandFactory(
					projectBuildType);
			factory.newInstance().execute(vo);

		} catch (Exception e) {
			PluginLoggerUtil.error(ID,
					Message.view_exception_uninstallepluginlist, e);
		}
	}

	public void updateCatalog(String projectLocation) {
		try {
			// 1. get project location
			String configFile = ConfigXmlUtil.getCommonConfigFile(projectLocation);
			ProjectConfig projectConfig = ConfigXmlUtil.getProjectConfig(configFile);
			String anyframeHomeLocation = projectConfig.getAnyframeHome();
			String projectName = projectConfig.getPjtName();
			String projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_ANT;
			if(StringUtil.isEmptyOrNull(projectConfig.getAnyframeHome())){
				projectBuildType = CommonConstants.PROJECT_BUILD_TYPE_MAVEN;
			}
			
			// 2. make arguments
			CommandVO vo = new CommandVO();
			vo.setCommand(CommandUtil.CMD_UPDATE_CATALOG);
			vo.setProjectHome(projectLocation);
			vo.setAnyframeHome(anyframeHomeLocation);
			vo.setBasedir(projectLocation);
			vo.setProjectName(projectName);

			// 3. run ant
			AbstractCommandFactory factory = new CommandFactory(
					projectBuildType);
			factory.newInstance().execute(vo);

		} catch (Exception e) {
			PluginLoggerUtil.error(ID, Message.view_exception_getpluginlist, e);
		}
	}
}
