<template>
  <div class="app-container" style="height: 900px">
    <el-container>
      <el-header>
        <el-tooltip content="格式化" placement="bottom">
          <el-button @click="formatJson" size="mini" type="info"
                     icon="el-icon-caret-right"
                     circle plain></el-button>
        </el-tooltip>
      </el-header>
      <el-container>
        <el-aside width="400px">
          <el-input type="textarea" rows="30" v-model="jsonInput"></el-input>
        </el-aside>
        <el-main class="padding0">
          <div v-show="jsonObject !== ''">
            <vue-json-pretty
              :data="jsonObject"
              :showLength=true
              :highlightMouseoverNode="true"
              @click="handleClick"
            />
          </div>
        </el-main>
      </el-container>
    </el-container>

    <span class="copyElement" :data-clipboard-target="copyData" @click="copy"></span>
  </div>
</template>

<script>
  import showdown from 'showdown'
  import request from '@/utils/request';
  import jsonlint from 'jsonlint'
  import VueJsonPretty from 'vue-json-pretty'
  import Clipboard from 'clipboard'
  import VueClipboard from 'vue-clipboard2'

  export default {
    name: 'jsonFormatView',
    components: {
      VueJsonPretty
    },
    data() {
      return {
        jsonInput: "",
        jsonObject: "",
        copyData: ""
      }
    },

    created() {
    },
    mounted() {
    },
    beforeDestroy() {

    },
    destroyed() {
    },

    watch: {},//watch
    computed: {},
    methods: {
      formatJson() {
        if (this.jsonInput !== undefined && this.jsonInput.trim() !== '') {
          try {
            this.jsonObject = jsonlint.parse(this.jsonInput);
          } catch (e) {
            this.$message({message: e.message, type: 'warning', showClose: true})
          }
        }
      },
      handleClick(path, data) {
        this.copyData = JSON.stringify(data);
        this.copy();
      },
      copy() {
        var clipboard = new Clipboard('.copyElement')
        clipboard.on('success', e => {
          this.$message({message: "复制成功", type: 'info', showClose: true})
          // 释放内存
          clipboard.destroy()
        });
        clipboard.on('error', e => {
          // 不支持复制
          this.$message({message: "该浏览器不支持自动复制", type: 'warning', showClose: true})
          // 释放内存
          clipboard.destroy()
        })
      }
    }
  }
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
  .padding0 {
    padding: 0 0 0 20px;
    margin: 0 auto;
  }
</style>
