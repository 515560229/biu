<template>
  <div class="app-container" style="height: 900px">
    <el-container>
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
                  <el-input type="textarea" rows="20" v-model="tableData['input' + index]"
                            placeholder="请输入二维码的内容"></el-input>
                </el-aside>
                <el-main width="300px" class="padding0">
                  <div v-if="qrCodeUrls.length > index">
                    <el-image
                      :src="qrCodeUrls[index]"></el-image>
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
  import jsonlint from 'jsonlint'
  import debounce from 'lodash/debounce'
  import VueJsonPretty from 'vue-json-pretty'
  import {jsonFormat} from '@/utils'

  export default {
    name: 'qrCodeView',
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
        //tabs相关
        currentTabName: "1",
        imgWidth: 400,
        tableData: {},
        qrCodeUrls: [],
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
      'tableData': {
        handler: debounce(function () {
          this.refresh();
        }),
        deep: true
      }
    },//watch
    computed: {},
    methods: {
      refresh() {
        let idx = parseInt(this.currentTabName) - 1;
        let escapeContent = encodeURIComponent(this.tableData['input' + idx]);
        this.qrCodeUrls.splice(idx, 1, process.env.BASE_API + "/qrCode/generate?width=" + this.imgWidth + "&height=" + this.imgWidth + "&content=" + escapeContent);
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
