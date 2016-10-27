/**
 * Copyright 2005-2016 hdiv.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hdiv.urlProcessor;

import javax.servlet.http.HttpServletRequest;

import org.hdiv.AbstractHDIVTestCase;
import org.hdiv.config.HDIVConfig;

public class RandomNameTest extends AbstractHDIVTestCase {

	private LinkUrlProcessor linkUrlProcessor;

	@Override
	protected void postCreateHdivConfig(final HDIVConfig config) {
		config.setRandomName(true);
	}

	@Override
	protected void onSetUp() throws Exception {

		linkUrlProcessor = getApplicationContext().getBean(LinkUrlProcessor.class);
	}

	public void testProcessAction() {

		getConfig().setAvoidValidationInUrlsWithoutParams(Boolean.FALSE);

		HttpServletRequest request = getMockRequest();
		String url = "/testAction.do";

		String result = linkUrlProcessor.processUrl(request, url);

		assertTrue(result.startsWith(url));
		assertTrue(!result.contains("_HDIV_STATE_"));
	}

}
