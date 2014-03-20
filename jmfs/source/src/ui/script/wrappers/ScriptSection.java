/* 
	Java MFS (jmfs) - Copyright (C) 2010 Artem Erchov
	Contact: comer0@gmail.com 

	This file is part of jmfs.

	jmfs is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	jmfs is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package ui.script.wrappers;

import java.util.ArrayList;
import java.util.List;


public class ScriptSection {
	private String				name;
	private Class<?>			handler;
	private List<ScriptStep>	steps = new ArrayList<ScriptStep>();
	
	public ScriptSection(String name) {
		this.name = name;
	}
	
	public Class<?> getHandler() {
		return handler;
	}
	public void setHandler(Class<?> handler) {
		this.handler = handler;
	}
	public List<ScriptStep> getSteps() {
		return steps;
	}
	public void setSteps(List<ScriptStep> steps) {
		this.steps = steps;
	}
	public void addStep(ScriptStep step) {
		this.steps.add( step );
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
