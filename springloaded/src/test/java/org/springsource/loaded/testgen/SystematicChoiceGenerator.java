/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded.testgen;

import java.util.ArrayList;
import java.util.List;

public class SystematicChoiceGenerator implements IChoiceGenerator {

	boolean firstChoice = true;

	/**
	 * Record choices in here so they can be retrieved afterwards
	 */
	public List<Boolean> choices = new ArrayList<Boolean>();

	public int next;

	public SystematicChoiceGenerator() {
	}

	public SystematicChoiceGenerator(List<Boolean> replayChoices) {
		replay(replayChoices);
	}

	public boolean nextBoolean() {
		Boolean chosen = null;
		if (next < choices.size()) {
			chosen = choices.get(next++);
		}
		else {
			chosen = firstChoice;
			choices.add(chosen);
			next++;
		}
		return chosen;
	}

	/**
	 * Should be called when reusing this choice generator. It will produce a test run that makes identical choices than
	 * the previous run.
	 */
	public void restart() {
	}

	/**
	 * Reinitialise the state of the choice generator to replay a given list of choices.
	 */
	private void replay(List<Boolean> replayChoices) {
		this.choices = replayChoices;
		this.next = 0;
	}

	/**
	 * Call this method to 'advance' the predetermined choices array by one (this will do a kind of backtracking
	 * starting by trying to change the last choice for which the alternative has not been tried yet.
	 * <p>
	 * 
	 * @return true if backtracking was successful, false if all options where explored.
	 */
	public boolean backtrack() {
		//First throw away any unused replayable choice bits beyond the 'next' pointer. 
		replay(new ArrayList<Boolean>(choices.subList(0, next))); //Note this must be a copy, not a view, to avoid mutation of
																	//'remembered' choices for replay.
		//Backtrack to the last choice we can change:
		int last = choices.size() - 1;
		while (last >= 0) {
			if (choices.get(last) == firstChoice) {
				choices.set(last, !firstChoice);
				return true; //Found another choice to explore
			}
			else {
				choices.remove(last);
				last--;
			}
		}
		return false; //No more choices can be changed
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < choices.size(); i++) {
			if (i == next)
				result.append(" next-->");
			result.append(choices.get(i) ? '1' : '0');
		}
		return result.toString();
	}

}
