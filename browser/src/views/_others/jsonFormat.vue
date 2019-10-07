<template>
  <div class="app-container" style="height: 900px">
    <el-container>
      <el-header>
          <el-button type="primary" size="mini" @click="formatJson">格式化</el-button>
          <el-button type="primary" size="mini" @click="formatJsonWithRemoveTransferredMeaning">去转义格式化</el-button>
      </el-header>
      <el-container>
        <el-aside width="400px">
          <el-input type="textarea" rows="30" v-model="jsonInput" placeholder="请输入要格式化的json"></el-input>
        </el-aside>
        <el-main class="padding0">
          <div v-show="jsonObject !== ''">
            <vue-json-pretty
              :data="jsonObject"
              :showLength=true
              :highlightMouseoverNode="true"
            />
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
    import jsonlint from 'jsonlint'
    import debounce from 'lodash/debounce'
    import VueJsonPretty from 'vue-json-pretty'

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

        watch: {
            'jsonInput': debounce(function () {
                this.formatJsonWithRemoveTransferredMeaning();
            }, 500)
        },//watch
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
            renderJson(obj) {
                for (let key in obj) {
                    if (typeof obj[key] === "string") {
                        try {
                            let value = jsonlint.parse(obj[key]);
                            //如果string转换后是合不法的对象,则将该key指定这个对象
                            obj[key] = value;
                            this.renderJson(value);
                        } catch (e) {
                            //do nothing
                        }
                    }
                }
            },
            formatJsonWithRemoveTransferredMeaning() {
                if (this.jsonInput !== undefined && this.jsonInput.trim() !== '') {
                    try {
                        let tempObj = jsonlint.parse(this.jsonInput);
                        this.renderJson(tempObj);
                        this.jsonObject = tempObj;
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
