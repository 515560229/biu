<template>
  <div class="app-container" style="height: 900px">
    <el-container>
      <el-header>
        <el-button type="primary" size="mini" @click="formatXml">格式化</el-button>
        <el-button type="primary" size="mini" @click="formatXmlWithRemoveTransferredMeaning">
          去转义格式化
        </el-button>
      </el-header>
      <el-container>
        <el-aside width="400px">
          <el-input type="textarea" rows="30" v-model="xmlInput" placeholder="请输入要格式化的xml"></el-input>
        </el-aside>
        <el-main class="padding0">
          <div v-show="xmlResult !== ''">
            <el-input type="textarea" rows="30" v-model="xmlResult"></el-input>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
    import debounce from 'lodash/debounce'
    let pd = require('pretty-data').pd;

    export default {
        name: 'xmlFormatView',
        components: {},
        data() {
            return {
                xmlInput: "",
                xmlResult: "",
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

        watch: {
            'xmlInput': debounce(function () {
                this.formatXmlWithRemoveTransferredMeaning();
            }, 500)
        },//watch
        computed: {},
        methods: {
            formatXml() {
                if (this.xmlInput !== undefined && this.xmlInput.trim() !== '') {
                    try {
                        this.xmlResult = pd.xml(this.xmlInput);
                    } catch (e) {
                        this.$message({message: e.message, type: 'warning', showClose: true})
                    }
                }
            },
            formatXmlWithRemoveTransferredMeaning() {
                if (this.xmlInput !== undefined && this.xmlInput.trim() !== '') {
                    try {
                        this.xmlResult = pd.xml(this.xmlInput.replace(new RegExp("&nbsp;", "g"), "")
                            .replace(new RegExp("&lt;", "g"),"<")
                            .replace(new RegExp("&gt;", "g"),">")
                            .replace(new RegExp("&amp;", "g"),"&")
                            .replace(new RegExp("&quot;", "g"),"\"")
                            .replace(new RegExp("&apos;", "g"),"'")
                            .replace(new RegExp("&times;", "g"),"×")
                            .replace(new RegExp("&divde;", "g"),"÷")
                        );
                    } catch (e) {
                        this.$message({message: e.message, type: 'warning', showClose: true})
                    }
                }
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
