/*
 * Copyright 2008-2011 the original author or authors.
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
package org.anyframe.ide.command.common.plugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This is an PluginBuild class. This object is converted from plugin resource
 * META-INF/anyframe/plugin.xml or plugin-build.xml using XStream. This object
 * is related to <build> in plugin.xml or plugin-build.xml
 * 
 * <pre>
 * Example:
 * <build>
 *  <filesets>
 * 	    <fileset dir="src/main/java" filtered="true" packaged="true">
 * 		    <exclude name="**\/org\/anyframe\/plugin\/mip\/query\/**\/*.java" />
 * 	    </fileset>
 *      ...
 *  </filesets>
 * </build>
 * </pre>
 * 
 * @author SoYon Lim
 */
public class PluginBuild implements Serializable {
	private static final long serialVersionUID = 1L;

	private List<Fileset> filesets;

	public List<Fileset> getFilesets() {
		if (this.filesets == null) {
			return new ArrayList<Fileset>();
		}
		return filesets;
	}

	public void setFilesets(List<Fileset> filesets) {
		this.filesets = filesets;
	}
}
