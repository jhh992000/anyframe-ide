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
 * This is an Include class. This object is converted from plugin resource
 * META-INF/anyframe/plugin.xml or plugin-build.xml using XStream. This object
 * is related to <include> in plugin.xml or plugin-build.xml
 * 
 * <pre>
 * Example:
 * <resources>
 * 	<resource dir="src/main/java" filtered="true" packaged="true">
 * 		<include name="**\/*.java" />
 * 	</resource>
 *  ...
 * </resources>
 * </pre>
 * 
 * @author SoYon Lim
 */
public class Include implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
