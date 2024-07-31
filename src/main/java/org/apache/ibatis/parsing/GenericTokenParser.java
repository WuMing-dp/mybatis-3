/*
 *    Copyright 2009-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

  // 开始标记
  private final String openToken;
  // 结束标记
  private final String closeToken;
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  /**
   * 解析openToken和closeToken之间的内容,并将解析结果交给handler处理
   * @param text 待解析的字符串
   * @return
   */
  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    int start = text.indexOf(openToken);
    if (start == -1) {
      // 没有找到开始标记，直接返回
      return text;
    }
    char[] src = text.toCharArray();
    //起始查找位置
    int offset = 0;
    final StringBuilder builder = new StringBuilder();
    StringBuilder expression = null;
    do {
      //转义字符
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
        //openToken的前一个字符是\，说明是转义字符，直接拼接
        //添加[offset, start - offset - 1)与openToken的内容
        builder.append(src, offset, start - offset - 1).append(openToken);
        offset = start + openToken.length();
        //不是转义字符
      } else {
        // found open token. let's search close token.
        //创建,重置expression
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        //添加offset和openToken之前的内容
        builder.append(src, offset, start - offset);
        offset = start + openToken.length();
        //查找结束标记
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          //非转义
          if ((end <= offset) || (src[end - 1] != '\\')) {
            expression.append(src, offset, end - offset);
            break;
          }
          // this close token is escaped. remove the backslash and continue.
          //转义的话,添加[offset, end - offset - 1)与closeToken的内容并继续查询
          expression.append(src, offset, end - offset - 1).append(closeToken);
          offset = end + closeToken.length();
          end = text.indexOf(closeToken, offset);
        }
        //拼接内容,没有找到结束就直接拼接
        if (end == -1) {
          // close token was not found.
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          // <x> closeToken 找到，将 expression 提交给 handler 处理 ，并将处理结果添加到 builder 中
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }
      start = text.indexOf(openToken, offset);
    } while (start > -1);
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
