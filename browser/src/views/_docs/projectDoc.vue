<template>
  <div class="app-container">
    <p v-html="message"></p>
  </div>
</template>

<script>
  import showdown from 'showdown'
  import request from '@/utils/request';

  export default {
    name: 'projectDoc',
    data() {
      return {
        message: ""
      }
    },

    created() {
    },
    mounted() {
      this.init();
    },
    beforeDestroy() {

    },
    destroyed() {
    },

    watch: {},//watch
    computed: {
    },
    methods: {
      init() {
        request({
          url: '/docs/get?name=README.md',
          method: 'get'
        }).then(res => {
          let md_content = res.data.message;
          let converter = new showdown.Converter();
          this.message = converter.makeHtml(md_content);
        })
      }
    }
  }
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
</style>
