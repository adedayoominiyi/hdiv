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
package org.hdiv.dataComposer;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hdiv.config.HDIVConfig;
import org.hdiv.context.RequestContext;
import org.hdiv.idGenerator.UidGenerator;
import org.hdiv.session.ISession;
import org.hdiv.state.IPage;
import org.hdiv.state.IParameter;
import org.hdiv.state.IState;
import org.hdiv.state.Page;
import org.hdiv.state.Parameter;
import org.hdiv.state.RandomTokenType;
import org.hdiv.util.Constants;
import org.hdiv.util.Method;
import org.springframework.web.util.HtmlUtils;

/**
 * <p>
 * It processes the data contributed by the HDIV custom tags. The aim of this class is to create an object of type IState for each possible
 * request (form or link) in every page processed by the HDIV custom tags. The IState object is used to validate client's later requests.
 * </p>
 * <p>
 * The process of creating an IState object is as follows: Each time a link or a form processing begins, HDIV custom tags set the request
 * beginning by calling beginRequest method. Once the beginning is set, an IState object is created and it is fill in with all the data of
 * the request(parameter values, non editable values, parameter types) using the compose method. After processing all the request data of
 * the link or form, custom tags set the end of the processing by calling endRequest method.
 * </p>
 *
 * @author Roberto Velasco
 */
public abstract class AbstractDataComposer implements IDataComposer {

	/**
	 * Commons Logging instance
	 */
	private static final Log log = LogFactory.getLog(AbstractDataComposer.class);

	/**
	 * Http session wrapper
	 */
	protected ISession session;

	/**
	 * Unique id generator
	 */
	protected UidGenerator uidGenerator;

	/**
	 * Page with the possible requests or states
	 */
	protected IPage page;

	/**
	 * States stack to store all states of the page <code>page</code>
	 */
	Deque<IState> states;

	/**
	 * HDIV configuration object.
	 */
	protected HDIVConfig hdivConfig;

	/**
	 * Context holder for request-specific state.
	 */
	protected RequestContext context;

	private final StringBuilder sb = new StringBuilder(128);

	public AbstractDataComposer(final RequestContext context) {
		this.context = context;
	}

	/**
	 * DataComposer initialization with new stack to store all states of the page <code>page</code>.
	 */
	public void init() {
		setPage(new Page(0));
		states = new ArrayDeque<IState>();
	}

	protected Deque<IState> getStates() {
		return states;
	}

	@Deprecated
	protected final Deque<IState> getStatesStack() {
		return states;
	}

	/**
	 * Obtains a new unique identifier for the page.
	 *
	 * @param parentStateId Parent state id
	 */
	public void initPage(final String parentStateId) {
		setPage(new Page(session.getPageId(context)));
		page.setParentStateId(parentStateId);
	}

	/**
	 * Obtains a new unique identifier for the page.
	 */
	public void initPage() {
		initPage(null);
	}

	/**
	 * It generates a new encoded value for the parameter <code>parameter</code> and the value <code>value</code> passed as parameters. The
	 * returned value guarantees the confidentiality in the memory strategy if confidentiality indicator <code>confidentiality</code> is
	 * true.
	 *
	 * @param parameter HTTP parameter name
	 * @param value value generated by server
	 * @param editable parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @return Codified value to send to the client
	 */
	public String compose(final String parameter, final String value, final boolean editable) {
		return compose(parameter, value, editable, false);
	}

	/**
	 * It generates a new encoded value for the parameter <code>parameter</code> and the value <code>value</code> passed as parameters. The
	 * returned value guarantees the confidentiality in the memory strategy if confidentiality indicator <code>confidentiality</code> is
	 * true.
	 *
	 * @param action target action
	 * @param parameter HTTP parameter name
	 * @param value value generated by server
	 * @param editable parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @return Codified value to send to the client
	 */
	public String compose(final String action, final String parameter, final String value, final boolean editable) {

		return compose(action, parameter, value, editable, false, Constants.ENCODING_UTF_8);
	}

	/**
	 * Adds a new IParameter object, generated from the values passed as parameters, to the current state <code>state</code>. If
	 * confidentiality is activated it generates a new encoded value that will be returned by the server for the parameter
	 * <code>parameter</code> in the memory strategy.
	 *
	 * @param parameter HTTP parameter
	 * @param value value generated by server
	 * @param editable Parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @param isActionParam parameter added in action attribute
	 * @return Codified value to send to the client
	 */
	public String compose(final String parameter, final String value, final boolean editable, final boolean isActionParam) {

		return compose(parameter, value, editable, isActionParam, Constants.ENCODING_UTF_8);
	}

	/**
	 * It generates a new encoded value for the parameter <code>parameter</code> and the value <code>value</code> passed as parameters. The
	 * returned value guarantees the confidentiality in the memory strategy if confidentiality indicator <code>confidentiality</code> is
	 * true.
	 *
	 * @param parameter HTTP parameter name
	 * @param value value generated by server
	 * @param editable parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @param editableName editable name (text or textarea)
	 * @return Codified value to send to the client
	 * @since HDIV 1.1
	 */
	public String compose(final String parameter, final String value, final boolean editable, final String editableName) {

		return compose(parameter, value, editable, editableName, false, null, Constants.ENCODING_UTF_8);
	}

	/**
	 * It generates a new encoded value for the parameter <code>parameter</code> and the value <code>value</code> passed as parameters. The
	 * returned value guarantees the confidentiality in the memory strategy if confidentiality indicator <code>confidentiality</code> is
	 * true.
	 *
	 * @param action target action
	 * @param parameter parameter name
	 * @param value value generated by server
	 * @param editable parameter type: editable(textbox, password,etc.) or non editable (hidden, select,...)
	 * @param isActionParam parameter added in action attribute
	 * @param charEncoding character encoding
	 * @return Codified value to send to the client
	 */
	public String compose(final String action, final String parameter, final String value, final boolean editable,
			final boolean isActionParam, final String charEncoding) {

		// Get current IState
		IState state = states.peek();
		if (state.getAction() != null && state.getAction().trim().length() == 0) {
			state.setAction(action);
		}
		return compose(parameter, value, editable, isActionParam, charEncoding);
	}

	/**
	 * Adds a new IParameter object, generated from the values passed as parameters, to the current state <code>state</code>. If
	 * confidentiality is activated it generates a new encoded value that will be returned by the server for the parameter
	 * <code>parameter</code> in the memory strategy.
	 *
	 * @param parameter HTTP parameter
	 * @param value value generated by server
	 * @param editable Parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @param isActionParam parameter added in action attribute
	 * @param charEncoding character encoding
	 * @return Codified value to send to the client
	 */
	public String compose(final String parameter, final String value, final boolean editable, final boolean isActionParam,
			final String charEncoding) {

		return compose(parameter, value, editable, null, isActionParam, null, charEncoding);
	}

	/**
	 * Adds a new IParameter object, generated from the values passed as parameters, to the current state <code>state</code>. If
	 * confidentiality is activated it generates a new encoded value that will be returned by the server for the parameter
	 * <code>parameter</code> in the memory strategy.
	 *
	 * @param parameter HTTP parameter
	 * @param value value generated by server
	 * @param editable Parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @param editableName editable name (text or textarea)
	 * @param isActionParam parameter added in action attribute
	 * @param method http method, GET or POST
	 * @return Codified value to send to the client
	 * @since HDIV 2.1.5
	 */
	public String compose(final String parameter, final String value, final boolean editable, final String editableName,
			final boolean isActionParam, final Method method) {
		return compose(parameter, value, editable, editableName, isActionParam, method, Constants.ENCODING_UTF_8);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hdiv.dataComposer.IDataComposer#composeParams(java.lang.String, java.lang.String, java.lang.String)
	 */
	public final String composeParams(String parameters, final Method method, final String charEncoding) {

		if (parameters == null || parameters.length() == 0) {
			return null;
		}

		// Get current IState
		IState state = states.peek();
		state.setParams(parameters);

		if (hdivConfig.getConfidentiality()) {
			// replace real values with confidential ones
			parameters = applyConfidentialityToParams(parameters, method);
		}

		return parameters;
	}

	/**
	 * Apply confidentiality to parameters String. Replaces real values with confidential ones.
	 *
	 * @param parameters parameters in query format
	 * @param method HTTP method
	 * @return parameters in query format with confidential values
	 */
	protected String applyConfidentialityToParams(String parameters, final Method method) {

		Map<String, Integer> pCount = new HashMap<String, Integer>();

		parameters = parameters.replaceAll("&amp;", "&");
		String newParameters = parameters;

		// Init indexes
		int beginIndex = 0;
		int endIndex = parameters.indexOf('&') > 0 ? parameters.indexOf('&') : parameters.length();
		do {
			String param = parameters.substring(beginIndex, endIndex);
			int index = param.indexOf('=');
			index = index < 0 ? param.length() : index;
			String name = param.substring(0, index);

			if (isConfidentialParam(name, method)) {
				// Parameter is not a start parameter
				Integer count = pCount.get(name);
				int num = (count == null) ? 0 : count + 1;
				pCount.put(name, num);

				// Replace parameter with confidential values
				newParameters = newParameters.replace(param, name + "=" + num);
			}

			// Update indexes
			beginIndex = endIndex + 1;
			endIndex = parameters.indexOf('&', endIndex + 1);
			if (endIndex < 0) {
				endIndex = parameters.length();
			}

		} while (endIndex > beginIndex);

		return newParameters;
	}

	/**
	 * Adds a new IParameter object, generated from the values passed as parameters, to the current state <code>state</code>. If
	 * confidentiality is activated it generates a new encoded value that will be returned by the server for the parameter
	 * <code>parameter</code> in the memory strategy.
	 * <p>
	 * Custom method for form field.
	 *
	 * @param parameter HTTP parameter
	 * @param value value generated by server
	 * @param editable Parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @param editableName editable name (text or textarea)
	 * @return Codified value to send to the client
	 * @since HDIV 2.1.5
	 */
	public String composeFormField(final String parameter, final String value, final boolean editable, final String editableName) {

		return compose(parameter, value, editable, editableName, false, Method.POST, Constants.ENCODING_UTF_8);
	}

	/**
	 * Adds a new IParameter object, generated from the values passed as parameters, to the current state <code>state</code>. If
	 * confidentiality is activated it generates a new encoded value that will be returned by the server for the parameter
	 * <code>parameter</code> in the memory strategy.
	 *
	 * @param parameterName HTTP parameter
	 * @param value value generated by server
	 * @param editable Parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @param editableName editable name (text or textarea)
	 * @param isActionParam parameter added in action attribute
	 * @param method http method, GET or POST
	 * @param charEncoding character encoding
	 * @return Codified value to send to the client
	 * @since HDIV 2.1.5
	 */
	public String compose(final String parameterName, final String value, final boolean editable, final String editableName,
			final boolean isActionParam, Method method, final String charEncoding) {

		if (!isRequestStarted()) {
			// If request not started, do nothing
			return value;
		}

		if (method == null) {
			// Default method is GET
			method = Method.GET;
		}

		IParameter parameter = composeParameter(parameterName, value, editable, editableName, isActionParam, charEncoding);

		if (isConfidentialParam(parameterName, method)) {
			return parameter.getConfidentialValue();
		}
		else {
			return value;
		}

	}

	/**
	 * Returns true if the parameter requires confidentiality. False otherwise.
	 *
	 * @param parameterName the name of the parameter
	 * @param method request HTTP method
	 * @return boolean result
	 * @since HDIV 2.1.6
	 */
	protected boolean isConfidentialParam(final String parameterName, final Method method) {

		if (!hdivConfig.getConfidentiality()) {
			return false;
		}

		if (hdivConfig.isStartParameter(parameterName)) {
			return false;
		}

		if (isUserDefinedNonValidationParameter(parameterName)) {
			return false;
		}

		if (hdivConfig.isParameterWithoutConfidentiality(context.getRequest(), parameterName)) {
			return false;
		}

		return true;
	}

	/**
	 * Checks if the parameter <code>parameter</code> is defined by the user as a no required validation parameter for the action
	 * <code>this.target</code>.
	 *
	 * @param parameter parameter name
	 * @return True If it is parameter that needs no validation. False otherwise.
	 * @since HDIV 2.0.6
	 */
	protected boolean isUserDefinedNonValidationParameter(final String parameter) {
		String action = states.peek().getAction();

		if (hdivConfig.isParameterWithoutValidation(action, parameter)) {

			if (log.isDebugEnabled()) {
				log.debug("parameter " + parameter + " doesn't need validation. It is user defined parameter.");
			}
			return true;
		}
		return false;
	}

	/**
	 * Adds a new IParameter object, generated from the values passed as parameters, to the current state <code>state</code>.
	 *
	 * @param parameterName HTTP parameter
	 * @param value value generated by server
	 * @param editable Parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @param editableDataType editable parameter name (text or textarea)
	 * @param isActionParam parameter added in action attribute
	 * @param charEncoding character encoding
	 * @return Codified value to send to the client
	 * @since HDIV 1.1
	 */
	protected IParameter composeParameter(final String parameterName, final String value, final boolean editable,
			final String editableDataType, final boolean isActionParam, final String charEncoding) {

		// we decoded value before store it in state.
		String decodedValue = null;
		if (!editable) {
			decodedValue = getDecodedValue(value, charEncoding);
		}

		// Get current IState
		IState state = states.peek();

		IParameter parameter = state.getParameter(parameterName);
		if (parameter != null) {
			if (parameter.isEditable() != editable) {
				// A parameter can be created as editable but if a new non editable value is added, the parameter is
				// changed to non editable. This is required in some frameworks like Struts 2.
				parameter.setEditable(editable);
			}
			parameter.addValue(decodedValue);
		}
		else {
			// create a new parameter and add to the request
			parameter = createParameter(parameterName, decodedValue, editable, editableDataType, isActionParam, charEncoding);
			state.addParameter(parameter);
		}

		return parameter;
	}

	/**
	 * Instantiates the parameter
	 *
	 * @param parameterName name of the parameter
	 * @param decodedValue the decoded value of the parameter
	 * @param editable Parameter type: editable(textbox, password,etc.) or non editable (hidden, select, radio, ...)
	 * @param editableDataType editable parameter name (text or textarea)
	 * @param isActionParam parameter added in action attribute
	 * @param charEncoding character encoding
	 * @return New IParameter object
	 */
	protected IParameter createParameter(final String parameterName, final String decodedValue, final boolean editable,
			final String editableDataType, final boolean isActionParam, final String charEncoding) {
		return new Parameter(parameterName, decodedValue, editable, editableDataType, isActionParam);
	}

	/**
	 * Creates a new parameter called <code>newParameter</code> and adds all the values of <code>oldParameter</code> stored in the state to
	 * it.
	 *
	 * @param oldParameter name of the parameter stored in the state
	 * @param newParameter name of the new parameter
	 */
	public void mergeParameters(final String oldParameter, final String newParameter) {

		// Get current IState
		IState state = states.peek();
		IParameter storedParameter = state.getParameter(oldParameter);

		if (!storedParameter.getValues().isEmpty()) {

			IParameter parameter = composeParameter(newParameter, storedParameter.getValuePosition(0), false, "", false,
					Constants.ENCODING_UTF_8);

			String currentValue = null;
			// We check the parameters since the second position because the first
			// value has been used to create the parameter
			for (int i = 1; i < storedParameter.getValues().size(); i++) {
				currentValue = storedParameter.getValuePosition(i);
				parameter.addValue(currentValue);
			}
		}
	}

	/**
	 * <p>
	 * Decoded <code>value</code> using input <code>charEncoding</code>.
	 * </p>
	 * <p>
	 * Removes Html Entity elements too. Like that:
	 * </p>
	 * <blockquote> &amp;#<i>Entity</i>; - <i>(Example: &amp;amp;) case sensitive</i> &amp;#<i>Decimal</i>; - <i>(Example: &amp;#68;)</i>
	 * <br>
	 * &amp;#x<i>Hex</i>; - <i>(Example: &amp;#xE5;) case insensitive</i><br>
	 * </blockquote>
	 * <p>
	 * Based on {@link HtmlUtils#htmlUnescape}.
	 * </p>
	 *
	 * @param value value to decode
	 * @param charEncoding character encoding
	 * @return value decoded
	 */
	protected String getDecodedValue(final String value, final String charEncoding) {

		if (value == null || value.length() == 0) {
			return "";
		}

		String decodedValue = null;
		try {
			decodedValue = URLDecoder.decode(value, charEncoding);
		}
		catch (UnsupportedEncodingException e) {
			decodedValue = value;
		}
		catch (IllegalArgumentException e) {
			decodedValue = value;
		}

		// Remove escaped Html elements
		if (decodedValue.indexOf('&') != -1) {
			// Can contain escaped characters
			decodedValue = HtmlUtils.htmlUnescape(decodedValue);
		}

		return decodedValue;
	}

	/**
	 * True if beginRequest has been executed and endRequest not.
	 *
	 * @return boolean
	 */
	public boolean isRequestStarted() {
		return !states.isEmpty();
	}

	/**
	 * Adds the flow identifier to the page of type <code>IPage</code>.
	 *
	 * @since HDIV 2.0.3
	 */
	public void addFlowId(final String id) {
		page.setFlowId(id);
	}

	/**
	 * Obtains the suffix to add to the _HDIV_STATE_ parameter in the memory strategy.
	 *
	 * @param type Random token type
	 *
	 * @return Returns suffix added to the _HDIV_STATE_ parameter.
	 * @since 2.1.7
	 */
	protected final String getStateSuffix(final RandomTokenType type) {
		String randomToken = page.getRandomToken(type);
		if (randomToken == null) {
			randomToken = uidGenerator.generateUid().toString();
			page.setRandomToken(randomToken, type);
		}
		return randomToken;
	}

	/**
	 * @param session the session to setg
	 */
	public void setSession(final ISession session) {
		this.session = session;
	}

	/**
	 * @return the page
	 */
	public final IPage getPage() {
		return page;
	}

	/**
	 * @param page the page to set
	 */
	public void setPage(final IPage page) {
		this.page = page;
	}

	/**
	 * @param uidGenerator the uidGenerator to set
	 */
	public void setUidGenerator(final UidGenerator uidGenerator) {
		this.uidGenerator = uidGenerator;
	}

	/**
	 * @param hdivConfig The HDIV configuration object to set.
	 */
	public void setHdivConfig(final HDIVConfig hdivConfig) {
		this.hdivConfig = hdivConfig;
	}

	public StringBuilder getBuilder() {
		return sb;
	}
}
