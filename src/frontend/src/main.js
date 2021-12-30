import '@babel/polyfill'
import 'mutationobserver-shim'
import Vue from 'vue'
import './plugins/bootstrap-vue'
import App from './App.vue'
import axios from 'axios'
import VueRouter from "vue-router"
import Dashboard from './components/Dashboard.vue'

Vue.use({
  install(Vue) {
    Vue.prototype.axios = axios.create()
  }
})

const router = new VueRouter({
  mode: "history",
  routes: [
    { path: '/', component: Dashboard },
  ],
  scrollBehavior: function (to, from, savedPosition) {
    if (to.hash) {
      return {
        selector: to.hash
      }
    } else if (savedPosition) {
      return savedPosition;
    } else {
      return {
        x: 0,
        y: 0
      }
    }
  },
})

Vue.config.productionTip = false
new Vue({
  router,
  render: h => h(App),
}).$mount('#app')
