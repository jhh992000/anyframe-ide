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
package org.anyframe.ide.command.common.plugin;

import java.io.Serializable;

/**
 * This is an Exclude class. This object is converted from plugin resource
 * META-INF/anyframe/plugin.xml or plugin-build.xml using XStream. This object
 * is related to <exclude> in plugin.xml or plugin-build.xml.
 * 
 * <pre>
 * Example:
 * <resources>
 * 	<resource dir="src/main/java" filtered="true" packaged="true">
 * 		<exclude name="**\/*.xml" merged="true"/>
 * 	</resource>
 *  ...
 * </resources>
 * </pre>
 * 
 * @author SoYon Lim
 */
public class Exclude implements Serializable {
	private static final long serialVersionUID = 1L;
	private String name;
	private boolean merged = false;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isMerged() {
		return merged;
	}

	public void setMerged(boolean merged) {
		this.merged = merged;
	}
	
}
