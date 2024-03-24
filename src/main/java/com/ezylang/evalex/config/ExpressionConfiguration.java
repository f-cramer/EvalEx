/*
  Copyright 2012-2022 Udo Klimaschewski

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package com.ezylang.evalex.config;

import com.ezylang.evalex.data.DataAccessorIfc;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.data.MapBasedDataAccessor;
import com.ezylang.evalex.data.conversion.DefaultEvaluationValueConverter;
import com.ezylang.evalex.data.conversion.EvaluationValueConverterIfc;
import com.ezylang.evalex.functions.FunctionIfc;
import com.ezylang.evalex.functions.basic.*;
import com.ezylang.evalex.functions.datetime.*;
import com.ezylang.evalex.functions.string.StringContains;
import com.ezylang.evalex.functions.string.StringLowerFunction;
import com.ezylang.evalex.functions.string.StringUpperFunction;
import com.ezylang.evalex.functions.trigonometric.*;
import com.ezylang.evalex.operators.OperatorIfc;
import com.ezylang.evalex.operators.arithmetic.*;
import com.ezylang.evalex.operators.booleans.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;

/**
 * The expression configuration can be used to configure various aspects of expression parsing and
 * evaluation. <br>
 * A <code>Builder</code> is provided to create custom configurations, e.g.: <br>
 *
 * <pre>
 *   ExpressionConfiguration config = ExpressionConfiguration.builder().mathContext(MathContext.DECIMAL32).arraysAllowed(false).build();
 * </pre>
 *
 * <br>
 * Additional operators and functions can be added to an existing configuration:<br>
 *
 * <pre>
 *     ExpressionConfiguration.defaultConfiguration()
 *        .withAdditionalOperators(
 *            new AbstractMap.SimpleEntry<>("++", new PrefixPlusPlusOperator()),
 *            new AbstractMap.SimpleEntry<>("++", new PostfixPlusPlusOperator()))
 *        .withAdditionalFunctions(new AbstractMap.SimpleEntry<>("save", new SaveFunction()),
 *            new AbstractMap.SimpleEntry<>("update", new UpdateFunction()));
 * </pre>
 */
@Builder(toBuilder = true)
public class ExpressionConfiguration {

  /** The standard set constants for EvalEx. */
  public static final Map<String, EvaluationValue> StandardConstants =
      Collections.unmodifiableMap(getStandardConstants());

  /** Setting the decimal places to unlimited, will disable intermediate rounding. */
  public static final int DECIMAL_PLACES_ROUNDING_UNLIMITED = -1;

  /** The default math context has a precision of 68 and {@link RoundingMode#HALF_EVEN}. */
  public static final MathContext DEFAULT_MATH_CONTEXT =
      new MathContext(68, RoundingMode.HALF_EVEN);

  /**
   * The default date time formatters used when parsing a date string. Each format will be tried and
   * the first matching will be used.
   *
   * <ul>
   *   <li>{@link DateTimeFormatter#ISO_DATE_TIME}
   *   <li>{@link DateTimeFormatter#ISO_DATE}
   *   <li>{@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
   *   <li>{@link DateTimeFormatter#ISO_LOCAL_DATE}
   * </ul>
   */
  protected static final List<DateTimeFormatter> DEFAULT_DATE_TIME_FORMATTERS =
      new ArrayList<>(
          Arrays.asList(
              DateTimeFormatter.ISO_DATE_TIME,
              DateTimeFormatter.ISO_DATE,
              DateTimeFormatter.ISO_LOCAL_DATE_TIME,
              DateTimeFormatter.ISO_LOCAL_DATE,
              DateTimeFormatter.RFC_1123_DATE_TIME));

  /** The operator dictionary holds all operators that will be allowed in an expression. */
  @Builder.Default
  @Getter
  @SuppressWarnings("unchecked")
  private final OperatorDictionaryIfc operatorDictionary =
      MapBasedOperatorDictionary.ofOperators(
          // arithmetic
          new AbstractMap.SimpleEntry<>("+", new PrefixPlusOperator()),
          new AbstractMap.SimpleEntry<>("-", new PrefixMinusOperator()),
          new AbstractMap.SimpleEntry<>("+", new InfixPlusOperator()),
          new AbstractMap.SimpleEntry<>("-", new InfixMinusOperator()),
          new AbstractMap.SimpleEntry<>("*", new InfixMultiplicationOperator()),
          new AbstractMap.SimpleEntry<>("/", new InfixDivisionOperator()),
          new AbstractMap.SimpleEntry<>("^", new InfixPowerOfOperator()),
          new AbstractMap.SimpleEntry<>("%", new InfixModuloOperator()),
          // booleans
          new AbstractMap.SimpleEntry<>("=", new InfixEqualsOperator()),
          new AbstractMap.SimpleEntry<>("==", new InfixEqualsOperator()),
          new AbstractMap.SimpleEntry<>("!=", new InfixNotEqualsOperator()),
          new AbstractMap.SimpleEntry<>("<>", new InfixNotEqualsOperator()),
          new AbstractMap.SimpleEntry<>(">", new InfixGreaterOperator()),
          new AbstractMap.SimpleEntry<>(">=", new InfixGreaterEqualsOperator()),
          new AbstractMap.SimpleEntry<>("<", new InfixLessOperator()),
          new AbstractMap.SimpleEntry<>("<=", new InfixLessEqualsOperator()),
          new AbstractMap.SimpleEntry<>("&&", new InfixAndOperator()),
          new AbstractMap.SimpleEntry<>("||", new InfixOrOperator()),
          new AbstractMap.SimpleEntry<>("!", new PrefixNotOperator()));

  /** The function dictionary holds all functions that will be allowed in an expression. */
  @Builder.Default
  @Getter
  @SuppressWarnings("unchecked")
  private final FunctionDictionaryIfc functionDictionary =
      MapBasedFunctionDictionary.ofFunctions(
          // basic functions
          new AbstractMap.SimpleEntry<>("ABS", new AbsFunction()),
          new AbstractMap.SimpleEntry<>("CEILING", new CeilingFunction()),
          new AbstractMap.SimpleEntry<>("COALESCE", new CoalesceFunction()),
          new AbstractMap.SimpleEntry<>("FACT", new FactFunction()),
          new AbstractMap.SimpleEntry<>("FLOOR", new FloorFunction()),
          new AbstractMap.SimpleEntry<>("IF", new IfFunction()),
          new AbstractMap.SimpleEntry<>("LOG", new LogFunction()),
          new AbstractMap.SimpleEntry<>("LOG10", new Log10Function()),
          new AbstractMap.SimpleEntry<>("MAX", new MaxFunction()),
          new AbstractMap.SimpleEntry<>("MIN", new MinFunction()),
          new AbstractMap.SimpleEntry<>("NOT", new NotFunction()),
          new AbstractMap.SimpleEntry<>("RANDOM", new RandomFunction()),
          new AbstractMap.SimpleEntry<>("ROUND", new RoundFunction()),
          new AbstractMap.SimpleEntry<>("SUM", new SumFunction()),
          new AbstractMap.SimpleEntry<>("SQRT", new SqrtFunction()),
          // trigonometric
          new AbstractMap.SimpleEntry<>("ACOS", new AcosFunction()),
          new AbstractMap.SimpleEntry<>("ACOSH", new AcosHFunction()),
          new AbstractMap.SimpleEntry<>("ACOSR", new AcosRFunction()),
          new AbstractMap.SimpleEntry<>("ACOT", new AcotFunction()),
          new AbstractMap.SimpleEntry<>("ACOTH", new AcotHFunction()),
          new AbstractMap.SimpleEntry<>("ACOTR", new AcotRFunction()),
          new AbstractMap.SimpleEntry<>("ASIN", new AsinFunction()),
          new AbstractMap.SimpleEntry<>("ASINH", new AsinHFunction()),
          new AbstractMap.SimpleEntry<>("ASINR", new AsinRFunction()),
          new AbstractMap.SimpleEntry<>("ATAN", new AtanFunction()),
          new AbstractMap.SimpleEntry<>("ATAN2", new Atan2Function()),
          new AbstractMap.SimpleEntry<>("ATAN2R", new Atan2RFunction()),
          new AbstractMap.SimpleEntry<>("ATANH", new AtanHFunction()),
          new AbstractMap.SimpleEntry<>("ATANR", new AtanRFunction()),
          new AbstractMap.SimpleEntry<>("COS", new CosFunction()),
          new AbstractMap.SimpleEntry<>("COSH", new CosHFunction()),
          new AbstractMap.SimpleEntry<>("COSR", new CosRFunction()),
          new AbstractMap.SimpleEntry<>("COT", new CotFunction()),
          new AbstractMap.SimpleEntry<>("COTH", new CotHFunction()),
          new AbstractMap.SimpleEntry<>("COTR", new CotRFunction()),
          new AbstractMap.SimpleEntry<>("CSC", new CscFunction()),
          new AbstractMap.SimpleEntry<>("CSCH", new CscHFunction()),
          new AbstractMap.SimpleEntry<>("CSCR", new CscRFunction()),
          new AbstractMap.SimpleEntry<>("DEG", new DegFunction()),
          new AbstractMap.SimpleEntry<>("RAD", new RadFunction()),
          new AbstractMap.SimpleEntry<>("SIN", new SinFunction()),
          new AbstractMap.SimpleEntry<>("SINH", new SinHFunction()),
          new AbstractMap.SimpleEntry<>("SINR", new SinRFunction()),
          new AbstractMap.SimpleEntry<>("SEC", new SecFunction()),
          new AbstractMap.SimpleEntry<>("SECH", new SecHFunction()),
          new AbstractMap.SimpleEntry<>("SECR", new SecRFunction()),
          new AbstractMap.SimpleEntry<>("TAN", new TanFunction()),
          new AbstractMap.SimpleEntry<>("TANH", new TanHFunction()),
          new AbstractMap.SimpleEntry<>("TANR", new TanRFunction()),
          // string functions
          new AbstractMap.SimpleEntry<>("STR_CONTAINS", new StringContains()),
          new AbstractMap.SimpleEntry<>("STR_LOWER", new StringLowerFunction()),
          new AbstractMap.SimpleEntry<>("STR_UPPER", new StringUpperFunction()),
          // date time functions
          new AbstractMap.SimpleEntry<>("DT_DATE_NEW", new DateTimeNewFunction()),
          new AbstractMap.SimpleEntry<>("DT_DATE_PARSE", new DateTimeParseFunction()),
          new AbstractMap.SimpleEntry<>("DT_DATE_FORMAT", new DateTimeFormatFunction()),
          new AbstractMap.SimpleEntry<>("DT_DATE_TO_EPOCH", new DateTimeToEpochFunction()),
          new AbstractMap.SimpleEntry<>("DT_DURATION_NEW", new DurationNewFunction()),
          new AbstractMap.SimpleEntry<>(
              "DT_DURATION_FROM_MILLIS", new DurationFromMillisFunction()),
          new AbstractMap.SimpleEntry<>("DT_DURATION_TO_MILLIS", new DurationToMillisFunction()),
          new AbstractMap.SimpleEntry<>("DT_DURATION_PARSE", new DurationParseFunction()));

  /** The math context to use. */
  @Builder.Default @Getter private final MathContext mathContext = DEFAULT_MATH_CONTEXT;

  /**
   * The data accessor is responsible for accessing variable and constant values in an expression.
   * The supplier will be called once for each new expression, the default is to create a new {@link
   * MapBasedDataAccessor} instance for each expression, providing a new storage for each
   * expression.
   */
  @Builder.Default @Getter
  private final Supplier<DataAccessorIfc> dataAccessorSupplier = MapBasedDataAccessor::new;

  /**
   * Default constants will be added automatically to each expression and can be used in expression
   * evaluation.
   */
  @Builder.Default @Getter
  private final Map<String, EvaluationValue> defaultConstants = getStandardConstants();

  /** Support for arrays in expressions are allowed or not. */
  @Builder.Default @Getter private final boolean arraysAllowed = true;

  /** Support for structures in expressions are allowed or not. */
  @Builder.Default @Getter private final boolean structuresAllowed = true;

  /** Support for implicit multiplication, like in (a+b)(b+c) are allowed or not. */
  @Builder.Default @Getter private final boolean implicitMultiplicationAllowed = true;

  /** Support for single quote string literals, like in 'Hello World' are allowed or not. */
  @Builder.Default @Getter private final boolean singleQuoteStringLiteralsAllowed = false;

  /**
   * The power of operator precedence, can be set higher {@link
   * OperatorIfc#OPERATOR_PRECEDENCE_POWER_HIGHER} or to a custom value.
   */
  @Builder.Default @Getter
  private final int powerOfPrecedence = OperatorIfc.OPERATOR_PRECEDENCE_POWER;

  /**
   * If specified, only the final result of the evaluation will be rounded to the specified number
   * of decimal digits, using the MathContexts rounding mode.
   *
   * <p>The default value of _DECIMAL_PLACES_ROUNDING_UNLIMITED_ will disable rounding.
   */
  @Builder.Default @Getter
  private final int decimalPlacesResult = DECIMAL_PLACES_ROUNDING_UNLIMITED;

  /**
   * If specified, all results from operations and functions will be rounded to the specified number
   * of decimal digits, using the MathContexts rounding mode.
   *
   * <p>Automatic rounding is disabled by default. When enabled, EvalEx will round all input
   * variables, constants, intermediate operation and function results and the final result to the
   * specified number of decimal digits, using the current rounding mode. Using a value of
   * _DECIMAL_PLACES_ROUNDING_UNLIMITED_ will disable automatic rounding.
   */
  @Builder.Default @Getter
  private final int decimalPlacesRounding = DECIMAL_PLACES_ROUNDING_UNLIMITED;

  /**
   * If set to true (default), then the trailing decimal zeros in a number result will be stripped.
   */
  @Builder.Default @Getter private final boolean stripTrailingZeros = true;

  /**
   * If set to true (default), then variables can be set that have the name of a constant. In that
   * case, the constant value will be removed and a variable value will be set.
   */
  @Builder.Default @Getter private final boolean allowOverwriteConstants = true;

  /** The time zone id. By default, the system default zone ID is used. */
  @Builder.Default @Getter private final ZoneId zoneId = ZoneId.systemDefault();

  /**
   * The date-time formatters. When parsing, each format will be tried and the first matching will
   * be used. For formatting, only the first will be used.
   *
   * <p>By default, the {@link ExpressionConfiguration#DEFAULT_DATE_TIME_FORMATTERS} are used.
   */
  @Builder.Default @Getter
  private final List<DateTimeFormatter> dateTimeFormatters = DEFAULT_DATE_TIME_FORMATTERS;

  /** The converter to use when converting different data types to an {@link EvaluationValue}. */
  @Builder.Default @Getter
  private final EvaluationValueConverterIfc evaluationValueConverter =
      new DefaultEvaluationValueConverter();

  /**
   * Convenience method to create a default configuration.
   *
   * @return A configuration with default settings.
   */
  public static ExpressionConfiguration defaultConfiguration() {
    return ExpressionConfiguration.builder().build();
  }

  /**
   * Adds additional operators to this configuration.
   *
   * @param operators variable number of arguments with a map entry holding the operator name and
   *     implementation. <br>
   *     Example: <code>
   *        ExpressionConfiguration.defaultConfiguration()
   *          .withAdditionalOperators(
   *            new AbstractMap.SimpleEntry<>("++", new PrefixPlusPlusOperator()),
   *            new AbstractMap.SimpleEntry<>("++", new PostfixPlusPlusOperator()));
   *     </code>
   * @return The modified configuration, to allow chaining of methods.
   */
  @SafeVarargs
  public final ExpressionConfiguration withAdditionalOperators(
      Map.Entry<String, OperatorIfc>... operators) {
    Arrays.stream(operators)
        .forEach(entry -> operatorDictionary.addOperator(entry.getKey(), entry.getValue()));
    return this;
  }

  /**
   * Adds additional functions to this configuration.
   *
   * @param functions variable number of arguments with a map entry holding the functions name and
   *     implementation. <br>
   *     Example: <code>
   *        ExpressionConfiguration.defaultConfiguration()
   *          .withAdditionalFunctions(
   *            new AbstractMap.SimpleEntry<>("save", new SaveFunction()),
   *            new AbstractMap.SimpleEntry<>("update", new UpdateFunction()));
   *     </code>
   * @return The modified configuration, to allow chaining of methods.
   */
  @SafeVarargs
  public final ExpressionConfiguration withAdditionalFunctions(
      Map.Entry<String, FunctionIfc>... functions) {
    Arrays.stream(functions)
        .forEach(entry -> functionDictionary.addFunction(entry.getKey(), entry.getValue()));
    return this;
  }

  private static Map<String, EvaluationValue> getStandardConstants() {

    Map<String, EvaluationValue> constants = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    constants.put("TRUE", EvaluationValue.booleanValue(true));
    constants.put("FALSE", EvaluationValue.booleanValue(false));
    constants.put(
        "PI",
        EvaluationValue.numberValue(
            new BigDecimal(
                "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679")));
    constants.put(
        "E",
        EvaluationValue.numberValue(
            new BigDecimal(
                "2.71828182845904523536028747135266249775724709369995957496696762772407663")));
    constants.put("NULL", EvaluationValue.nullValue());

    constants.put(
        "DT_FORMAT_ISO_DATE_TIME",
        EvaluationValue.stringValue("yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]['['VV']']"));
    constants.put(
        "DT_FORMAT_LOCAL_DATE_TIME", EvaluationValue.stringValue("yyyy-MM-dd'T'HH:mm:ss[.SSS]"));
    constants.put("DT_FORMAT_LOCAL_DATE", EvaluationValue.stringValue("yyyy-MM-dd"));

    return constants;
  }
}
