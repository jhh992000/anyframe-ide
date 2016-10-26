/*
 * Copyright 2002-2012 the original author or authors.
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
package org.anyframe.spring.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.NumberUtils;

/**
 * Converts from a String to any JDK-standard Number implementation.
 * 
 * <p>
 * Support Number classes including Byte, Short, Integer, Float, Double, Long,
 * BigInteger, BigDecimal. This class delegates to
 * {@link NumberUtils#parseNumber(String, Class)} to perform the conversion.
 * 
 * Extends <code>StringToNumberConverterFactory</code> to provide converting
 * from "" to zero.
 * 
 * We changed
 * org.springframework.core.convert.support.StringToNumberConverterFactory Class
 * into org.anyframe.spring.converter.StringToNumberConverterFactory Class in Anyframe.
 * <br>
 * <br>
 * 
 * @author Keith Donald
 * @author modified by Jeryeon Kim
 * @since 1.0.0 
 * 
 * @see java.lang.Byte
 * @see java.lang.Short
 * @see java.lang.Integer
 * @see java.lang.Long
 * @see java.math.BigInteger
 * @see java.lang.Float
 * @see java.lang.Double
 * @see java.math.BigDecimal
 * @see NumberUtils
 */
public class StringToNumberConverterFactory implements
		ConverterFactory<String, Number> {

	public <T extends Number> Converter<String, T> getConverter(
			Class<T> targetType) {
		return new StringToNumber<T>(targetType);
	}

	private static final class StringToNumber<T extends Number> implements
			Converter<String, T> {

		private final Class<T> targetType;

		public StringToNumber(Class<T> targetType) {
			this.targetType = targetType;
		}

		public T convert(String source) {
			if (source.length() == 0) {
				return NumberUtils.parseNumber("0", this.targetType);
			}
			return NumberUtils.parseNumber(source, this.targetType);
		}
	}
}