/* License added by: GRADLE-LICENSE-PLUGIN
 *
 *    Copyright 2012 Jeroen van Erp (jeroen@javadude.nl)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package nl.javadude.assumeng;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class AssumptionListener extends BaseTestListener {
	@Override
	public void beforeInvocation(IInvokedMethod invokedMethod, ITestResult result) {
		ITestNGMethod testNgMethod = result.getMethod();
		Method method = testNgMethod.getConstructorOrMethod().getMethod();
		if (method == null || !method.isAnnotationPresent(Assumption.class)) {
			return;
		}

		if (!checkAssumptions(method, result)) {
			result.setStatus(ITestResult.SKIP);
		}
	}

	private boolean checkAssumptions(Method method, ITestResult result) {
		Assumption annotation = method.getAnnotation(Assumption.class);
		String[] assumptionMethods = annotation.methods();
		List<String> failedAssumptions = new ArrayList<String>();
		Class clazz = result.getMethod().getTestClass().getRealClass();
		boolean assumptionsHold = true;
		for (String assumptionMethod : assumptionMethods) {
			boolean assume = checkAssumption(result, clazz, assumptionMethod);
			if (!assume) {
				failedAssumptions.add(assumptionMethod);
			}
			assumptionsHold &= assume;
		}

		if (!assumptionsHold) {
			logger.warn("Skipping [{}] because the {} assumption(s) do not hold.", method.getName(), failedAssumptions);
		}
		return assumptionsHold;
	}

	private boolean checkAssumption(ITestResult result, Class clazz, String assumptionMethod) {
		try {
			Method assumption = clazz.getMethod(assumptionMethod);
			if (assumption.getReturnType() != boolean.class) {
				throw new RuntimeException(format("Assumption method [%s] should return a boolean", assumptionMethod));
			}
			return (Boolean) assumption.invoke(result.getInstance());
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(format("Could not find method [%s] to run assumption", assumptionMethod), e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(format("Could not invoke method [%s] to run assumption", assumptionMethod), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(format("Could not access method [%s] to run assumption", assumptionMethod), e);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(AssumptionListener.class);
}
