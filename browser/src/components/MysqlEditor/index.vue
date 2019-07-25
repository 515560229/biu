<template>
  <div class="mysql-editor">
    <textarea ref="textarea"></textarea>
  </div>
</template>

<script>
  // 更多API https://codemirror.net/doc/manual.html#api
  import CodeMirror from 'codemirror'
  import 'codemirror/lib/codemirror.css'
  // 我这里引入的是SQL语言文件
  import 'codemirror/mode/sql/sql.js'
  // 编辑的主题文件
  import 'codemirror/theme/base16-light.css'
  //编辑器代码高亮css文件
  import 'codemirror/addon/hint/show-hint.css'
  import 'codemirror/lib/codemirror.css'
  //代码折叠文件
  import 'codemirror/addon/fold/foldcode.js'
  import 'codemirror/addon/fold/foldgutter.js'
  import 'codemirror/addon/edit/matchbrackets.js'
  import 'codemirror/addon/fold/brace-fold.js'
  //选中行高亮文件
  import 'codemirror/addon/selection/active-line.js'
  //缩进文件
  import 'codemirror/addon/fold/indent-fold.js'
  //代码只能提示
  import 'codemirror/addon/hint/show-hint.js'
  import 'codemirror/addon/hint/sql-hint.js'
  //addon文件夹放的是Code Mirror的功能插件
  import 'codemirror/addon/fold/comment-fold.js'

  export default {
    name: 'mysqlEditor',
    data() {
      return {
        mysqlEditor: false,
        defauleOptions: {
          // 编辑器设置
          tabSize: 4,//tab大小
          mode: 'text/x-mysql',//编辑器模式支持文件
          theme: 'base16-light',//编辑器主题
          lineNumbers: true,//编辑器行号
          line: true,
          dragDrop: true,//拖拽
          lineWrapping: true, //代码折叠
          matchBrackets: true,  //括号匹配
          autofocus: true,//自动聚焦
          indentWithTabs: true,//首行缩进
          smartIndent: true,
          extraKeys: {"Ctrl": "autocomplete"},//ctrl唤起智能提示
          // more codemirror options, 更多 codemirror 的高级配置...
        }
      }
    },
    props: ['value', 'options'],
    watch: {
      value(value) {
        // console.log("value changed. " + value);
        const editor_value = this.mysqlEditor.getValue()
        if (value !== undefined && value !== null && value !== editor_value) {
          this.mysqlEditor.setValue(this.value)
        }
      }
    },
    mounted() {
      let _options = Object.assign(this.defauleOptions, this.options);//有属性相同时,后面的覆盖前面的. 属于浅拷贝
      this.mysqlEditor = CodeMirror.fromTextArea(this.$refs.textarea, _options);

      if (this.value !== undefined && this.value !== null) {
        this.mysqlEditor.setValue(this.value);
      }
      this.mysqlEditor.on('change', cm => {
        this.$emit('changed', cm.getValue());
        this.$emit('input', cm.getValue());
      })
    },
    methods: {
      getValue() {
        // console.log("getValue. " + this.mysqlEditor.getValue());
        return this.mysqlEditor.getValue()
      }
    }
  }
</script>

<style scoped>
  .mysql-editor {
    height: 100%;
    position: relative;
  }

  .mysql-editor >>> .CodeMirror {
    height: auto;
    min-height: 100px;
  }

  .mysql-editor >>> .CodeMirror-scroll {
    min-height: 100px;
  }

  .mysql-editor >>> .cm-s-rubyblue span.cm-string {
    color: #F08047;
  }
</style>
