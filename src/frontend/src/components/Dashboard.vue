<template>
  <b-container>
    <b-card class="m-2" text-variant="white" bg-variant="primary">
      <h2>HealthBuddy Dashboard</h2>
      HealthBuddy itself is
      <b-badge variant="success" v-if="healthBuddyAlive">up</b-badge>
      <b-badge variant="danger" v-if="!healthBuddyAlive">down</b-badge>
    </b-card>
    <b-container>
      <b-container
        v-for="environment in services"
        :key="environment.environmentName"
      >
        <h2>
          {{ environment.environmentName }}
          <span class="text-muted">
            environment ({{ environment.servicesUpCount }}/{{
              environment.servicesCount
            }})</span
          >
          <b-button
            v-b-toggle="'collapse-' + environment.environmentName"
            class="m-1"
          >
            <span class="when-open">Close</span
            ><span class="when-closed">Expand</span>
          </b-button>
        </h2>
        <b-collapse
          v-bind:id="'collapse-' + environment.environmentName"
          :visible="environment.servicesUpCount < environment.servicesCount"
        >
          <b-card
            v-for="service in environment.services"
            :key="service.id"
            :id="service.id"
            :bg-variant="service.isUp ? 'success' : 'danger'"
            text-variant="white"
            class="mb-1"
            header-tag="header"
            footer-tag="footer"
          >
            <template #header>
              <h3>
                {{ service.name }} <b-link :href="'#' + service.id">🔗</b-link>
              </h3>
            </template>
            <b-container v-if="!service.isUp">
              Incident type: {{ service.currentIncident.type }}<br />
              Incident start: {{ service.currentIncident.startDate }}<br />
              Resp. Status: {{ service.currentIncident.statusCode }}<br />
              Resp. Body: {{ service.currentIncident.body }}
            </b-container>
            <b-container v-if="service.incidentHistory">
              History (last {{ service.incidentHistory.historyMaximum }} min):
              <b-progress
                class="mt-2 border"
                :max="service.incidentHistory.historyMaximum"
                show-value
              >
                <b-progress-bar
                  v-for="entry in service.incidentHistory.history"
                  :key="entry.id"
                  :id="entry.id"
                  :value="entry.end - entry.start"
                  :label="entry.status"
                  :variant="mapIncidentHistoryStatusToColor(entry.status)"
                ></b-progress-bar>
              </b-progress>
              <b-tooltip
                v-for="entry in service.incidentHistory.history.filter(
                  (entry) => entry.incident
                )"
                :key="entry.id"
                :target="entry.id"
                triggers="hover"
              >
                Type: {{ entry.incident.type }}<br />
                Status code: {{ entry.incident.statusCode }}<br />
                Body: {{ entry.incident.body }}<br />
                Start date: {{ entry.incident.startDate }}<br />
                End date: {{ entry.incident.endDate }}
              </b-tooltip>
            </b-container>
            <template #footer>
              <b-row>
                <b-col class="text-left">
                  <b-link :href="service.url"
                    ><small class="text-light">{{ service.url }}</small></b-link
                  >
                </b-col>
                <b-col class="text-right">
                  <small class="text-light">{{ service.id }}</small>
                </b-col>
              </b-row>
            </template>
          </b-card>
        </b-collapse>
      </b-container>
    </b-container>
  </b-container>
</template>

<script>
export default {
  name: "Dashboard",
  data() {
    return {
      services: [],
      healthBuddyAlive: true,
    };
  },
  created() {
    this.updateData();
    this.timer = setInterval(this.updateData, 10000);
  },
  beforeDestroy() {
    this.cancelAutoUpdate();
  },
  methods: {
    updateData() {
      this.updateServiceStatus();
      this.updateHealthBuddyStatus();
    },
    updateServiceStatus() {
      this.axios
        .get("/api/v1/services", { timeout: 2000 })
        .then((response) => (this.services = response.data))
        .catch((error) => {
          console.log(error);
          this.services = [];
        });
    },
    async updateHealthBuddyStatus() {
      this.axios
        .get("/actuator/health", { timeout: 2000 })
        .then((this.healthBuddyAlive = true))
        .catch((error) => {
          console.log(error);
          this.healthBuddyAlive = false;
        });
    },
    cancelAutoUpdate() {
      clearInterval(this.timer);
    },
    mapIncidentHistoryStatusToColor(status) {
      if (status == "UP") {
        return "success";
      }
      if (status == "DOWN") {
        return "danger";
      }
      return "secondary";
    },
  },
};
</script>

<style>
.collapsed > .when-open,
.not-collapsed > .when-closed {
  display: none;
}
</style>

