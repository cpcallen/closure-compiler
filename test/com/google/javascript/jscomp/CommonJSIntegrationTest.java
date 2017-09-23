/*
 * Copyright 2013 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

import com.google.javascript.jscomp.deps.ModuleLoader;

/**
 * Tests for type-checking across commonjs modules.
 *
 * @author nicholas.j.santos@gmail.com (Nick Santos)
 */

public final class CommonJSIntegrationTest extends IntegrationTestCase {
  public void testCrossModuleCtorCall() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join("/** @constructor */ function Hello() {}", "module.exports = Hello;"),
          LINE_JOINER.join("var Hello = require('./i0');", "var hello = new Hello();")
        },
        new String[] {
          "var cjs_module$i0 = function (){};",
          LINE_JOINER.join("var Hello = cjs_module$i0;", "var hello = new cjs_module$i0();")
        });
  }

  public void testCrossModuleCtorCall2() {
    test(createCompilerOptions(),
         new String[] {
           "/** @constructor */ function Hello() {} " +
           "module.exports = Hello;",

           "var Hello = require('./i0');" +
           "var hello = new Hello(1);"
         },
         TypeCheck.WRONG_ARGUMENT_COUNT);
  }

  public void testCrossModuleTypeAnnotation() {
    test(
        createCompilerOptions(),
        LINE_JOINER.join(
            "/** @constructor */ function Hello() {} ",
            "/** @type {!Hello} */ var hello = new Hello();",
            "module.exports = Hello;"),
        LINE_JOINER.join(
            "var cjs_module$i0 = function () {};",
            "var hello$$cjs_module$i0 = new cjs_module$i0();"));
  }

  public void testCrossModuleTypeAnnotation2() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join("/** @constructor */ function Hello() {}", "module.exports = Hello;"),
          LINE_JOINER.join(
              "var Hello = require('./i0');", "/** @type {!Hello} */ var hello = new Hello();")
        },
        new String[] {
          "var cjs_module$i0 = function() {};",
          LINE_JOINER.join("var Hello = cjs_module$i0;", "var hello = new cjs_module$i0();")
        });
  }

  public void testCrossModuleTypeAnnotation3() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join("/** @constructor */ function Hello() {}", "module.exports = Hello;"),
          LINE_JOINER.join("var Hello = require('./i0');", "/** @type {!Hello} */ var hello = 1;")
        },
        TypeValidator.TYPE_MISMATCH_WARNING);
  }

  public void testMultipleExportAssignments1() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join(
              "/** @constructor */ function Hello() {}",
              "module.exports = Hello;",
              "/** @constructor */ function Bar() {}",
              "Bar.prototype.foobar = function() { alert('foobar'); };",
              "module.exports = Bar;"),
          LINE_JOINER.join(
              "var Foobar = require('./i0');", "var show = new Foobar();", "show.foobar();")
        },
        new String[] {
          LINE_JOINER.join(
              "var cjs_module$i0",
              "cjs_module$i0 = function () {};",
              "cjs_module$i0 = function () {};",
              "cjs_module$i0.prototype.foobar = function() { alert('foobar') };"),
          LINE_JOINER.join(
              "var Foobar = cjs_module$i0;", "var show = new cjs_module$i0();", "show.foobar();")
        });
  }

  public void testMultipleExportAssignments2() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join(
              "/** @constructor */ function Hello() {}",
              "module.exports.foo = Hello;",
              "/** @constructor */ function Bar() {} ",
              "Bar.prototype.foobar = function() { alert('foobar'); };",
              "module.exports.foo = Bar;"),
          LINE_JOINER.join(
              "var Foobar = require('./i0');", "var show = new Foobar.foo();", "show.foobar();")
        },
        new String[] {
          LINE_JOINER.join(
              "var cjs_module$i0 = {};",
              "cjs_module$i0.foo = function (){};",
              "cjs_module$i0.foo = function (){};",
              "cjs_module$i0.foo.prototype.foobar = function(){ alert('foobar') };"),
          LINE_JOINER.join(
              "var Foobar = cjs_module$i0;", "var show = new cjs_module$i0.foo();",
              "show.foobar();")
        });
  }

  public void testMultipleExportAssignments3() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join(
              "/** @constructor */ function Hello() {}",
              "module.exports.foo = Hello;",
              "/** @constructor */ function Bar() {} ",
              "Bar.prototype.foobar = function() { alert('foobar'); };",
              "exports.foo = Bar;"),
          LINE_JOINER.join(
              "var Foobar = require('./i0');", "var show = new Foobar.foo();", "show.foobar();")
        },
        new String[] {
          LINE_JOINER.join(
              "var cjs_module$i0 = {};",
              "cjs_module$i0.foo = function(){};",
              "cjs_module$i0.foo = function(){};",
              "cjs_module$i0.foo.prototype.foobar = function(){ alert('foobar') };"),
          LINE_JOINER.join(
              "var Foobar = cjs_module$i0;",
              "var show = new cjs_module$i0.foo();",
              "show.foobar();")
        });
  }

  public void testCrossModuleSubclass1() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join(
              "/** @constructor */ function Hello() {}",
              "module.exports = Hello;"),
          LINE_JOINER.join(
              "var Hello = require('./i0');",
              "var util = {inherits: function (x, y){}};",
              "/**\n",
              " * @constructor\n",
              " * @extends {Hello}\n",
              " */\n",
              "var SubHello = function () {};",
              "util.inherits(SubHello, Hello);")
        },
        new String[] {
          "var cjs_module$i0 = function (){};",
          LINE_JOINER.join(
              "var Hello = cjs_module$i0;",
              "var util = { inherits : function(x,y) {} };",
              "var SubHello = function() {};",
              "util.inherits(SubHello, cjs_module$i0);")
        });
  }

  public void testCrossModuleSubclass2() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join("/** @constructor */ function Hello() {}", "module.exports = Hello;"),
          LINE_JOINER.join(
              "var Hello = require('./i0');",
              "var util = {inherits: function (x, y){}};",
              "/**",
              " * @constructor",
              " * @extends {Hello}",
              " */",
              "function SubHello() {}",
              "util.inherits(SubHello, Hello);")
        },
        new String[] {
          "var cjs_module$i0 = function (){};",
          LINE_JOINER.join(
              "var Hello = cjs_module$i0;",
              "var util = { inherits : function(x,y) {} };",
              "function SubHello(){}",
              "util.inherits(SubHello, cjs_module$i0);")
        });
  }

  public void testCrossModuleSubclass3() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join("/** @constructor */ function Hello() {} ", "module.exports = Hello;"),
          LINE_JOINER.join(
              "var Hello = require('./i0');",
              "var util = {inherits: function (x, y){}};",
              "/**",
              " * @constructor",
              " * @extends {Hello}",
              " */",
              "function SubHello() { Hello.call(this); }",
              "util.inherits(SubHello, Hello);")
        },
        new String[] {
          "var cjs_module$i0 = function (){};",
          LINE_JOINER.join(
              "var Hello = cjs_module$i0;",
              "var util = { inherits : function(x,y) {} };",
              "function SubHello(){ cjs_module$i0.call(this); }",
              "util.inherits(SubHello, cjs_module$i0);")
        });
  }

  public void testCrossModuleSubclass4() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join(
              "/** @constructor */ function Hello() {} ", "module.exports = {Hello: Hello};"),
          LINE_JOINER.join(
              "var i0 = require('./i0');",
              "var util = {inherits: function (x, y) {}};",
              "/**",
              " * @constructor",
              " * @extends {i0.Hello}",
              " */",
              "function SubHello() { i0.Hello.call(this); }",
              "util.inherits(SubHello, i0.Hello);")
        },
        new String[] {
          LINE_JOINER.join(
              "/** @const */ var cjs_module$i0 = {};",
              "cjs_module$i0.Hello = /** @constructor */ function (){};"),
          LINE_JOINER.join(
              "var i0 = cjs_module$i0;",
              "var util = { inherits : function(x,y) {} };",
              "function SubHello(){ cjs_module$i0.Hello.call(this); }",
              "util.inherits(SubHello, cjs_module$i0.Hello);")
        });
  }

  public void testCrossModuleSubclass5() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join("/** @constructor */ function Hello() {}", "module.exports = Hello;"),
          LINE_JOINER.join(
              "var Hello = require('./i0');",
              "var util = {inherits: function (x, y){}};",
              "/**",
              " * @constructor",
              " * @extends {./i0}",
              " */",
              "function SubHello() { Hello.call(this); }",
              "util.inherits(SubHello, Hello);")
        },
        new String[] {
          "var cjs_module$i0 = function (){};",
          LINE_JOINER.join(
              "var Hello = cjs_module$i0;",
              "var util = { inherits : function(x,y) {} };",
              "function SubHello(){ cjs_module$i0.call(this); }",
              "util.inherits(SubHello, cjs_module$i0);")
        });
  }

  public void testCrossModuleSubclass6() {
    test(
        createCompilerOptions(),
        new String[] {
          LINE_JOINER.join(
              "/** @constructor */ function Hello() {}", "module.exports = {Hello: Hello};"),
          LINE_JOINER.join(
              "var i0 = require('./i0');",
              "var util = {inherits: function (x, y){}};",
              "/**",
              " * @constructor",
              " * @extends {./i0.Hello}",
              " */",
              "function SubHello() { i0.Hello.call(this); }",
              "util.inherits(SubHello, i0.Hello);")
        },
        new String[] {
          LINE_JOINER.join(
              "var cjs_module$i0 = {};",
              "cjs_module$i0.Hello = function (){};"),
          LINE_JOINER.join(
              "var i0 = cjs_module$i0;",
              "var util = {inherits:function(x,y){}};",
              "function SubHello(){ cjs_module$i0.Hello.call(this); }",
              "util.inherits(SubHello, cjs_module$i0.Hello);")
        });
  }

  public void testCrossModuleEs6CtorCall() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            LINE_JOINER.join("/** @constructor */ function Hello() {}", "export default Hello;"),
            LINE_JOINER.join("var Hello = require('./i0').default;", "var hello = new Hello();")
        },
        new String[] {
            LINE_JOINER.join(
                "var module$i0 = {};",
                "function Hello$$module$i0(){}",
                "var $jscompDefaultExport$$module$i0=Hello$$module$i0;",
                "module$i0.default=$jscompDefaultExport$$module$i0;",
                "var cjs_module$i0 = module$i0;"),
            LINE_JOINER.join(
                "var Hello = module$i0.default;",
                "var hello = new Hello();")
        });
  }

  public void testCrossModuleEs6CtorCall2() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            "/** @constructor */ function Hello() {} " +
                "export default Hello;",

            "var Hello = require('./i0').default;" +
                "var hello = new Hello(1);"
        },
        TypeCheck.WRONG_ARGUMENT_COUNT);
  }

  public void testCrossModuleEs6TypeAnnotation() {
    test(
        createEs6CompilerOptions(),
        LINE_JOINER.join(
            "/** @constructor */ function Hello() {} ",
            "/** @type {!Hello} */ var hello = new Hello();",
            "export default Hello;"),
        LINE_JOINER.join(
            "var module$i0 = {};",
            "function Hello$$module$i0(){}",
            "var hello$$module$i0 = new Hello$$module$i0();",
            "var $jscompDefaultExport$$module$i0 = Hello$$module$i0;",
            "module$i0.default = $jscompDefaultExport$$module$i0;",
            "var cjs_module$i0 = module$i0"));
  }

  public void testCrossModuleEs6TypeAnnotation2() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            LINE_JOINER.join("/** @constructor */ function Hello() {}", "export default Hello;"),
            LINE_JOINER.join(
                "var Hello = require('./i0').default;",
                "/** @type {!Hello} */ var hello = new Hello();")
        },
        new String[] {
            LINE_JOINER.join(
                "var module$i0 = {};",
                "function Hello$$module$i0(){}",
                "var $jscompDefaultExport$$module$i0 = Hello$$module$i0;",
                "module$i0.default = $jscompDefaultExport$$module$i0;",
                "var cjs_module$i0 = module$i0"),
            LINE_JOINER.join(
                "var Hello = module$i0.default;", "var hello = new Hello();")
        });
  }

  public void testCrossModuleEs6TypeAnnotation3() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            LINE_JOINER.join("/** @constructor */ function Hello() {}", "export default Hello;"),
            LINE_JOINER.join("var Hello = require('./i0').default;", "/** @type {!Hello} */ var hello = 1;")
        },
        TypeValidator.TYPE_MISMATCH_WARNING);
  }


  public void testCrossModuleEs6Subclass1() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            LINE_JOINER.join(
                "/** @constructor */ function Hello() {}",
                "export default Hello;"),
            LINE_JOINER.join(
                "var Hello = require('./i0').default;",
                "var util = {inherits: function (x, y){}};",
                "/**\n",
                " * @constructor\n",
                " * @extends {Hello}\n",
                " */\n",
                "var SubHello = function () {};",
                "util.inherits(SubHello, Hello);")
        },
        new String[] {
            LINE_JOINER.join(
                "var module$i0 = {};",
                "function Hello$$module$i0(){}",
                "var $jscompDefaultExport$$module$i0=Hello$$module$i0;",
                "module$i0.default=$jscompDefaultExport$$module$i0;",
                "var cjs_module$i0 = module$i0;"),
            LINE_JOINER.join(
                "var Hello = module$i0.default;",
                "var util = { inherits : function(x,y) {} };",
                "var SubHello = function() {};",
                "util.inherits(SubHello, Hello);")
        });
  }

  public void testCrossModuleEs6Subclass2() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            LINE_JOINER.join("/** @constructor */ function Hello() {}", "export default Hello;"),
            LINE_JOINER.join(
                "var Hello = require('./i0').default;",
                "var util = {inherits: function (x, y){}};",
                "/**",
                " * @constructor",
                " * @extends {Hello}",
                " */",
                "function SubHello() {}",
                "util.inherits(SubHello, Hello);")
        },
        new String[] {
            LINE_JOINER.join(
                "var module$i0 = {};",
                "function Hello$$module$i0(){}",
                "var $jscompDefaultExport$$module$i0=Hello$$module$i0;",
                "module$i0.default=$jscompDefaultExport$$module$i0;",
                "var cjs_module$i0 = module$i0;"),
            LINE_JOINER.join(
                "var Hello = module$i0.default;",
                "var util = { inherits : function(x,y) {} };",
                "function SubHello(){}",
                "util.inherits(SubHello, Hello);")
        });
  }

  public void testCrossModuleEs6Subclass3() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            LINE_JOINER.join("/** @constructor */ function Hello() {} ", "export default Hello;"),
            LINE_JOINER.join(
                "var Hello = require('./i0').default;",
                "var util = {inherits: function (x, y){}};",
                "/**",
                " * @constructor",
                " * @extends {Hello}",
                " */",
                "function SubHello() { Hello.call(this); }",
                "util.inherits(SubHello, Hello);")
        },
        new String[] {
            LINE_JOINER.join(
                "var module$i0 = {};",
                "function Hello$$module$i0(){}",
                "var $jscompDefaultExport$$module$i0=Hello$$module$i0;",
                "module$i0.default=$jscompDefaultExport$$module$i0;",
                "var cjs_module$i0 = module$i0;"),
            LINE_JOINER.join(
                "var Hello = module$i0.default;",
                "var util = { inherits : function(x,y) {} };",
                "function SubHello(){ Hello.call(this); }",
                "util.inherits(SubHello, Hello);")
        });
  }

  public void testCrossModuleEs6Subclass4() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            LINE_JOINER.join(
                "/** @constructor */ function Hello() {} ", "export { Hello };"),
            LINE_JOINER.join(
                "var i0 = require('./i0');",
                "var util = {inherits: function (x, y) {}};",
                "/**",
                " * @constructor",
                " * @extends {i0.Hello}",
                " */",
                "function SubHello() { i0.Hello.call(this); }",
                "util.inherits(SubHello, i0.Hello);")
        },
        new String[] {
            LINE_JOINER.join(
                "var module$i0 = {};",
                "function Hello$$module$i0(){}",
                "module$i0.Hello=Hello$$module$i0;",
                "var cjs_module$i0 = module$i0;"),
            LINE_JOINER.join(
                "var i0 = module$i0;",
                "var util = { inherits : function(x,y) {} };",
                "function SubHello(){ module$i0.Hello.call(this); }",
                "util.inherits(SubHello, module$i0.Hello);")
        });
  }

  public void testCrossModuleEs6Subclass5() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            LINE_JOINER.join("/** @constructor */ function Hello() {}", "export default Hello;"),
            LINE_JOINER.join(
                "var Hello = require('./i0').default;",
                "var util = {inherits: function (x, y){}};",
                "/**",
                " * @constructor",
                " * @extends {./i0}",
                " */",
                "function SubHello() { Hello.call(this); }",
                "util.inherits(SubHello, Hello);")
        },
        new String[] {
            LINE_JOINER.join(
                "var module$i0 = {};",
                "function Hello$$module$i0(){}",
                "var $jscompDefaultExport$$module$i0=Hello$$module$i0;",
                "module$i0.default=$jscompDefaultExport$$module$i0;",
                "var cjs_module$i0 = module$i0;"),
            LINE_JOINER.join(
                "var Hello = module$i0.default;",
                "var util = { inherits : function(x,y) {} };",
                "function SubHello(){ Hello.call(this); }",
                "util.inherits(SubHello, Hello);")
        });
  }

  public void testCrossModuleEs6Subclass6() {
    test(
        createEs6CompilerOptions(),
        new String[] {
            LINE_JOINER.join(
                "/** @constructor */ function Hello() {} ", "export { Hello };"),
            LINE_JOINER.join(
                "var i0 = require('./i0');",
                "var util = {inherits: function (x, y){}};",
                "/**",
                " * @constructor",
                " * @extends {./i0.Hello}",
                " */",
                "function SubHello() { i0.Hello.call(this); }",
                "util.inherits(SubHello, i0.Hello);")
        },
        new String[] {
            LINE_JOINER.join(
                "var module$i0 = {};",
                "function Hello$$module$i0(){}",
                "module$i0.Hello=Hello$$module$i0;",
                "var cjs_module$i0 = module$i0;"),
            LINE_JOINER.join(
                "var i0 = module$i0;",
                "var util = {inherits:function(x,y){}};",
                "function SubHello(){ module$i0.Hello.call(this); }",
                "util.inherits(SubHello, module$i0.Hello);")
        });
  }

  @Override
  protected CompilerOptions createCompilerOptions() {
    CompilerOptions options = new CompilerOptions();
    options.setCodingConvention(new GoogleCodingConvention());
    WarningLevel.VERBOSE.setOptionsForWarningLevel(options);
    options.setProcessCommonJSModules(true);
    options.setClosurePass(true);
    options.setModuleResolutionMode(ModuleLoader.ResolutionMode.NODE);
    return options;
  }

  protected CompilerOptions createEs6CompilerOptions() {
    CompilerOptions options = createCompilerOptions();
    options.setNewTypeInference(true);
    options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT_2015);
    options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5);
    return options;
  }
}
