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
        <el-main class="padding0">
          <el-tabs v-model="currentTabName" type="card">
            <el-tab-pane
              v-for="(item, index) in tabDatas"
              :key="item.name"
              :label="item.title"
              :name="item.name"
            >
              <span slot="label">{{item.title}}</span>
              <el-container>
                <el-aside width="400px">
                  <el-input type="textarea" rows="30" v-model="tableData['input' + index]"
                            placeholder="请输入要格式化的xml"></el-input>
                </el-aside>
                <el-main width="300px" class="padding0">
                  <div v-show="tableData['data' + index]">
                    <el-input type="textarea" rows="30" v-model="tableData['data' + index]"></el-input>
                  </div>
                </el-main>
              </el-container>
            </el-tab-pane>
          </el-tabs>
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
      let maxTabCount = 15;
      let initTabs = function () {
        let result = [];
        for (let i = 0; i < maxTabCount; i++) {
          let title = i + 1 + "";
          result.push({'title': title, 'name': title})
        }
        return result;
      };

      return {
        xmlInput: "",
        xmlResult: "",
        //tabs相关
        currentTabName: "1",
        tableData: {},
        tabDatas: initTabs()
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
      // 'xmlInput': debounce(function () {
      //   this.formatXmlWithRemoveTransferredMeaning();
      // }, 500)
    },//watch
    computed: {},
    methods: {
      formatXml() {
        let input = this.tableData['input' + parseInt(this.currentTabName - 1)];
        if (input !== undefined && input.trim() !== '') {
          try {
            let data = pd.xml(input);
            this.$set(this.tableData, "data" + (parseInt(this.currentTabName) - 1), data);
          } catch (e) {
            this.$message({message: e.message, type: 'warning', showClose: true})
          }
        }
      },
      formatXmlWithRemoveTransferredMeaning() {
        let input = this.tableData['input' + parseInt(this.currentTabName - 1)];
        if (input !== undefined && input.trim() !== '') {
          try {
            let data = pd.xml(input.replace(new RegExp("&nbsp;", "g"), "")
              .replace(new RegExp("&lt;", "g"), "<")
              .replace(new RegExp("&gt;", "g"), ">")
              .replace(new RegExp("&amp;", "g"), "&")
              .replace(new RegExp("&quot;", "g"), "\"")
              .replace(new RegExp("&apos;", "g"), "'")
              .replace(new RegExp("&times;", "g"), "×")
              .replace(new RegExp("&divde;", "g"), "÷")
            );
            this.$set(this.tableData, "data" + (parseInt(this.currentTabName) - 1), data);
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
