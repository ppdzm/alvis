package org.sa.alvis.completer

import jline.console.completer.StringsCompleter

/**
 * Created by Stuart Alex on 2017/9/8.
 */
case class SQLCompleter(completions: List[String]) extends StringsCompleter(completions.distinct: _*)