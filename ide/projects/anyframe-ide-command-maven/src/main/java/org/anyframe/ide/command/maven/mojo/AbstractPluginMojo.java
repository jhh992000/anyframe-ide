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
package org.anyframe.ide.command.maven.mojo;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import org.anyframe.ide.command.common.PluginInfoManager;
import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.ReflectionUtils;

/**
 * Base class for plugin mojo.
 * 
 * @author Soyon Lim
 */
public abstract class AbstractPluginMojo extends AbstractMojo {
	/**
	 * @component 
	 *            role="org.anyframe.ide.command.common.DefaultPluginInfoManager"
	 */
	protected PluginInfoManager pluginInfoManager;

	/**
	 * @parameter expression="${session}"
	 * @readonly
	 */
	protected MavenSession session;

	/**
	 * The archetype's groupId.
	 * 
	 * @parameter expression="${pluginGroupId}" default-value="anyframe.plugin"
	 */
	protected String pluginGroupId;

	/**
	 * The archetype's artifactId.
	 * 
	 * @parameter expression="${pluginArtifactId}"
	 */
	protected String pluginArtifactId;

	/**
	 * The archetype's version.
	 * 
	 * @parameter expression="${pluginVersion}"
	 */
	protected String pluginVersion;

	/**
	 * Local maven repository.
	 * 
	 * @parameter expression="${localRepository}"
	 * @readonly
	 */
	protected ArtifactRepository localRepository;

	/**
	 * The archetype's repository.
	 * 
	 * @parameter expression="${archetypeRepository}"
	 */
	protected String archetypeRepository;

	/**
	 * List of Remote Repositories used by the resolver.
	 * 
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 * @readonly
	 */
	@SuppressWarnings("unchecked")
	protected List remoteArtifactRepositories;

	/**
	 * target folder to install
	 * 
	 * @parameter expression="${basedir}"
	 */
	protected File baseDir;

	/**
	 * The plugin's encoding type.
	 * 
	 * @parameter expression="${encoding}"
	 */
	protected String encoding;

	/**
	 * The current user system settings for use in Maven.
	 * 
	 * @parameter default-value="${settings}"
	 * @required
	 * @readonly
	 */
	protected Settings settings;

	/**
	 * set local/remote repositories into requst
	 * 
	 * @param request
	 *            information includes maven repository, db settings, etc.
	 */
	@SuppressWarnings("unchecked")
	public void setRepository(ArchetypeGenerationRequest request)
			throws MojoExecutionException, MojoFailureException {

		request.setOutputDirectory(baseDir.getAbsolutePath());

		request.setLocalRepository(localRepository);
		request.setArchetypeRepository(archetypeRepository);
		request.setRemoteArtifactRepositories(remoteArtifactRepositories);

		List<Server> servers = settings.getServers();
		
		request.setActiveProxy(settings.getActiveProxy());

		for (Server server : servers) {
			request.addMirror(server);
		}

		getLog().debug(
				"Local repository is set to '" + localRepository.getBasedir()
						+ "'.");
		getLog().debug(
				"The size of remote repository is '"
						+ remoteArtifactRepositories.size() + "'.");
	}

	/**
	 * set values of mojo variables
	 * 
	 * @param mojo
	 *            mojo object
	 * @param variableName
	 *            variable
	 * @param value
	 *            variable value to be set
	 */
	public void setMojoVariable(Mojo mojo, String variableName, Object value)
			throws Exception {
		Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(
				variableName, mojo.getClass());
		field.setAccessible(true);
		field.set(mojo, value);
	}
}
