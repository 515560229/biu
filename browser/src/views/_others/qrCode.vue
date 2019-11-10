<template>
  <div class="app-container" style="height: 900px">
    <el-container>
      <el-container>
        <el-main>
          <el-tabs v-model="currentTabName" type="card">
            <el-tab-pane
              v-for="(item, index) in tabDatas"
              :key="item.name"
              :label="item.title"
              :name="item.name"
            >
              <span slot="label">{{item.title}}</span>
              <el-container>
                <el-header style="height: 150px;">
                  样式
                  <el-select v-model="tableData['style' + index]" placeholder="二维码"
                             style="width: 220px;">
                    <el-option
                      v-for="item in barcodeStyles"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value">
                    </el-option>
                  </el-select>
                  <p>
                    宽：
                    <el-input v-model="tableData['width' + index]" style="width: 80px;"
                              placeholder="400"></el-input>&nbsp;&nbsp;&nbsp;&nbsp;
                    高：
                    <el-input v-model="tableData['height' + index]" style="width: 80px;"
                              placeholder="400"></el-input>
                  <p/>
                  <el-input type="textarea" rows="2" v-model="tableData['input' + index]"
                            placeholder="请输入二维码的内容"></el-input>
                </el-header>
                <el-main>
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
        //默认的都是二维码的配置
        defaultWidth: 400,
        defaultHeight: 400,
        defaultStyle: "qrCode",
        tableData: {},
        qrCodeUrls: [],
        tabDatas: initTabs(),
        barcodeStyles: [
          {
            value: "qrCode",
            label: "二维码"
          },
          {
            value: "Code128",
            label: "条形码（128）"
          },
        ]
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
        if (this.tableData['input' + idx] === undefined || this.tableData['input' + idx] === '') {
          return;
        }
        let escapeContent = encodeURIComponent(this.tableData['input' + idx]);
        let width = this.tableData['width' + idx] === undefined || this.tableData['width' + idx] === '' ? this.defaultWidth : this.tableData['width' + idx];
        let height = this.tableData['height' + idx] === undefined || this.tableData['height' + idx] === '' ? this.defaultHeight : this.tableData['height' + idx];
        let style = this.tableData['style' + idx] === undefined || this.tableData['style' + idx] === '' ? this.defaultStyle : this.tableData['style' + idx];
        this.qrCodeUrls.splice(idx, 1, process.env.BASE_API + "/qrCode/generate?width=" + width + "&height=" + height + "&content=" + escapeContent + "&style=" + style);
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
