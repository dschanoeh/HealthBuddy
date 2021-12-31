<template>
  <b-container>
    <Dashboard />
    <Footer />
  </b-container>
</template>

<script>
import Dashboard from "./components/Dashboard.vue";
import Footer from "./components/Footer.vue";

export default {
  name: "App",
  components: {
    Dashboard,
    Footer,
  },
  created() {
    document.title = "HealthBuddy";
  },
};
</script>