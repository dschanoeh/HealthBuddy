<template>
  <Dashboard />
</template>

<script>
import Dashboard from "./components/Dashboard.vue";

export default {
  name: "App",
  components: {
    Dashboard,
  },
  created() {
    document.title = "HealthBuddy";
  },
};
</script>