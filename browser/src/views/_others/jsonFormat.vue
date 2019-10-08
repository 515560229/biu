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
          <el-tabs v-model="currentTabName" type="card">
            <el-tab-pane
              v-for="(item, index) in tabDatas"
              :key="item.name"
              :label="item.title"
              :name="item.name"
            >
              <span slot="label">{{item.title}}</span>
              <div v-show="tableData['data' + index]">
                <vue-json-pretty
                  :data="tableData['data' + index]"
                  :showLength=true
                  :highlightMouseoverNode="true"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
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
        jsonInput: "",
        jsonObject: "",
        //tabs相关
        currentTabName: "1",
        nextTabName: "2",
        tableData: {
        },
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
      // 'jsonInput': debounce(function () {
      //   this.formatJsonWithRemoveTransferredMeaning();
      // }, 500)
    },//watch
    computed: {},
    methods: {
      formatJson() {
        if (this.jsonInput !== undefined && this.jsonInput.trim() !== '') {
          try {
            let tempObj = jsonlint.parse(this.jsonInput);
            this.calcTabsAndFillData(tempObj);
          } catch (e) {
            this.$message({message: e.message, type: 'warning', showClose: true})
          }
        }
      },
      calcTabsAndFillData(data) {
        //设置当前tab的数据
        this.currentTabName = this.tableData['data' + parseInt(this.currentTabName - 1)] === undefined ? this.currentTabName : this.nextTabName;
        this.$set(this.tableData, "data" + (parseInt(this.currentTabName) - 1), data);
        //计算下个标签页
        this.nextTabName = parseInt(this.currentTabName) + 1 + "";
        if (parseInt(this.nextTabName) > this.tabDatas.length) {
          this.nextTabName = '1';//循环 超过则从1开始
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
            this.calcTabsAndFillData(tempObj);
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
