<template>
    <div id="main">
        <div v-if="this.loading" style="text-align: center">
            <b-spinner id="spinner" class="margin-bottom: 1.7em; margin-top: 0.8em" variant="primary" label="Spinning"></b-spinner>
        </div>
        <div v-else-if="this.errored" class="alert alert-danger alert-dismissible fade show " role="alert" id="alert">
            {{  error_message  }}
        </div>
        <div v-else-if="this.isRedirecting" class="alert alert-danger alert-dismissible fade show ">
            {{ redirectionMessage }}
        </div>
        <b-tabs v-else content-class="mt-4" justified>
            <b-tab title="Continuous" :active="continuousIsActive(true)">
                <section v-for="activity in this.activityList.filter(i => i.continuous)" :key="activity.id">
                    <!-- Activity List -->
                    <b-card border-variant="secondary" style="background-color: #f3f3f3" class="continuousCard">
                        <b-row no-gutters>
                            <b-col md="6">
                                <b-card-text>
                                    <strong>{{ activity.activity_name }} | {{ activity.creatorName }}</strong>
                                    <hr/>
                                    <div v-if="activity.description.length <= 100">
                                        {{ activity.description }}
                                    </div>
                                    <div v-else>
                                        {{ activity.description.substring(0,100)+"...." }}
                                    </div>
                                </b-card-text>
                            </b-col>
                            <b-col md="6">
                                <div class="activity-button-group float-right">
                                    <b-button-group vertical>
                                        <b-button v-if="activity.creatorUserId === activeUserId"
                                                  variant="outline-primary"
                                                  v-on:click="goToPage(`/activities/edit/${activity.id}`)">
                                            Edit
                                        </b-button>
                                        <b-button variant="outline-primary"
                                                  v-on:click="goToPage(`/activity/${activity.id}`)">
                                            Details
                                        </b-button>
                                        <b-button v-if="activity.creatorUserId === activeUserId"
                                                  variant="outline-danger"
                                                  v-on:click="deleteActivity(activity.id)">
                                            Delete
                                        </b-button>
                                    </b-button-group>
                                </div>
                            </b-col>
                        </b-row>
                    </b-card>
                    <br/>
                </section>
                <hr/>
                <footer class="noMore">No more activities to show</footer>
            </b-tab>
            <b-tab title="Duration" :active="continuousIsActive(false)">
                <section v-for="activity in this.activityList.filter(i => !i.continuous)" :key="activity.id">
                    <!-- Activity List -->
                    <b-card border-variant="secondary" style="background-color: #f3f3f3" class="durationCard">
                        <b-row no-gutters>
                            <b-col md="6">
                                <b-card-text :id="'activity' + activity.id + '-duration-card'">
                                    <strong>{{ activity.activity_name }} | {{ activity.creatorName }}</strong>
                                    <hr/>
                                    <strong>Start Date: </strong>{{ getDateTime(activity.start_time )}}
                                    <br/>
                                    <strong>End Date: </strong>{{ getDateTime(activity.end_time) }}
                                    <br/><br/>
                                    <div v-if="activity.description.length <= 100">
                                        {{ activity.description }}
                                    </div>
                                    <div v-else>
                                        {{ activity.description.substring(0,100)+"...." }}
                                    </div>
                                </b-card-text>
                            </b-col>
                            <b-col md="6">
                                <div class="activity-button-group float-right">
                                    <b-button-group vertical>
                                        <b-button v-if="activity.creatorUserId === activeUserId"
                                                  variant="outline-primary"
                                                  v-on:click="goToPage(`/activities/edit/${activity.id}`)">
                                            Edit
                                        </b-button>
                                        <b-button variant="outline-primary"
                                                  v-on:click="goToPage(`/activity/${activity.id}`)">
                                            Details
                                        </b-button>
                                        <b-button v-if="activity.creatorUserId === activeUserId"
                                                  variant="outline-danger"
                                                  v-on:click="deleteActivity(activity.id)">
                                            Delete
                                        </b-button>
                                    </b-button-group>
                                </div>
                            </b-col>
                        </b-row>
                        <br/>
                    </b-card>
                </section>
                <hr/>
                <footer class="noMore">No more activities to show</footer>
            </b-tab>
        </b-tabs>
    </div>
</template>

<script>
    import {formatDateTime} from "../../util";
    import api from "../../Api";

    export default {
        name: "ActivityList",
        props: {
            //this is an underscore as props need to be set to a data(){} attribute, not directly assigned
            // through a prop!!
            user_Id: {
                default: null
            }
        },
        data() {
            return {
                activityList : [],
                noMore: false,
                activeTab: 0,
                loading: true,
                userId: this.user_Id,
                errored: false,
                error_message: "Could not load user activities",
                activeUserId: null,
                isRedirecting: false,
                redirectionMessage: '',
                timeout: 3200
            }
        },
        beforeMount() {
            this.checkLoggedIn();
        },
        async mounted() {
            await this.getActiveUserId();
            await this.getListOfActivities();
            await this.getCreatorNamesForActivities();
            await this.getFollowingStatusForActivity();
        },
        methods: {
            /**
             * Makes a request to the back-end to FOLLOW an activity.
             *
             * @param activityId Id of the activity to follow
             * @param filteredIndex Index of activity to follow
             * @param isContinuous Type of Activity to follow
             * @returns {Promise<void>}
             */
            async followActivity(activityId, filteredIndex, isContinuous) {
                await api.setUserSubscribed(activityId, this.activeUserId).then(() => {
                    let activity = this.activityList.filter(activity => activity.continuous === isContinuous)[filteredIndex];
                    let index = this.activityList.indexOf(activity);
                    this.activityList[index].subscribed = true;
                    this.$forceUpdate();  // Notice we have to use a $ here
                }).catch((error) => {
                    this.processGetError(error, "FOLLOW");
                });
            },


            /**
             * Makes a request to the back-end to UNFOLLOW an activity.
             *
             * @param activityId Id of the activity to unfollow
             * @param filteredIndex Index of activity to unfollow
             * @param isContinuous Type of Activity to follow
             * @returns {Promise<void>}
             */
            async unfollowActivity(activityId, filteredIndex, isContinuous) {
                await api.deleteUserSubscribed(activityId, this.activeUserId).then(() => {
                    let activity = this.activityList.filter(activity => activity.continuous === isContinuous)[filteredIndex];
                    let index = this.activityList.indexOf(activity);
                    this.activityList[index].subscribed = false;
                    this.$forceUpdate();  // Notice we have to use a $ here
                }).catch((error) => {
                    this.processGetError(error, "UNFOLLOW");
                });
            },


            /**
             * This helper function is called when an error is caught
             * when performing a FOLLOW/UNFOLLOW requests to the server.<br>
             * Conditions handled are:<br>
             * 400 (BAD REQUEST) refreshes page,<br>
             * 401 (UNAUTHORIZED) redirect to login page,<br>
             * 403 (FORBIDDEN) and refreshes page,
             * - If user ever gets to another user's activity page and follows/unfollows their activity
             * Otherwise unknown error so refreshes page
             *
             * @param error error to be processed
             * @param feed String that indicates if it is an UNFOLLOW/FOLLOW/UNKNOWN feed
             */
            async processGetError(error, feed) {
                this.isRedirecting = true;
                if (error.response.status === 400) {
                    if (feed === "FOLLOW") {
                        this.redirectionMessage = "User can't follow an event they're already participating in, \n" +
                            "Refreshing page.";
                        setTimeout(() => {
                            this.$router.go();
                        }, this.timeout);
                    } else if (feed === "UNFOLLOW") {
                        this.redirectionMessage = "User can't unfollow an event they're not participating in, \n" +
                            "Refreshing page.";
                        setTimeout(() => {
                            this.$router.go();
                        }, this.timeout);
                    }
                }
                else if (error.response.status === 401) {
                    this.redirectionMessage = "Sorry, you are no longer logged in,\n" +
                        "Redirecting to the login page.";
                    setTimeout(() => {
                        this.logout()
                    }, this.timeout);
                } else if (error.response.status === 403) {
                    this.redirectionMessage = "Sorry, you are not allowed to access another user's activites,\n" +
                        "Refreshing page.";
                    setTimeout(() => {
                        this.$router.go();
                    }, this.timeout);
                } else {
                    this.redirectionMessage = "Something went wrong!,\n" +
                        "Refreshing page.";
                    setTimeout(() => {
                        this.$router.go();
                    }, this.timeout);
                }
            },


            /**
             * Obtains user id of person currently logged in.
             */
            getActiveUserId() {
                api.getUserId().then(response => {
                    this.activeUserId = response.data;
                }).catch()
            },

            /**
             * Imports formatDateTime function from util.js
             */
            getDateTime: formatDateTime,

            /**
             * Returns the starting/ending days associated with an duration activity
             */
            getDays(activity) {
                return Math.floor(((new Date(activity.end_time) - new Date(activity.start_time))/1000/60/60/24))
            },

            /**
             * Returns the starting/ending hours associated with an duration activity
             */
            getHours(activity) {
                return Math.ceil(((new Date(activity.end_time) - new Date(activity.start_time))/1000/60/60)) % 24
            },

            /**
             * Checks if a user is logged in
             */
            checkLoggedIn() {
                if (!sessionStorage.getItem("token")) {
                    this.$router.push("/login");
                }
            },


            /**
             * Retrieves a list of activities
             * that a user is following or has created
             */
            async getListOfActivities() {
                this.errored = false;
                await api.getUserActivities(this.userId).then(
                    response => { //If successfully registered the response will have a status of 201
                        if (response.data.length === 0) {
                            this.noMore = true;
                        }
                        this.activityList = response.data;
                    }).catch(() => {
                    this.errored = true;
                })
            },


            goToPage(url) {
                this.$router.push(url);
            },


            /**
             * When the page is loaded, make the tab active that has activities.  If the non-continuous tab has
             * activities, but the continuous tab doesn't, make the non-continuous tab active.
             * Else make continuous active.
             * @param isContinuousTab true if called by continuous tab, false of non-continuous tab.
             */
            continuousIsActive(isContinuousTab) {
                if (this.activityList.filter(a => !a.continuous).length > 0 && this.activityList.filter(a => a.continuous).length === 0) {
                    return !isContinuousTab;
                } else {
                    return isContinuousTab;
                }
            },


            /**
             * Opens a dialog box (modal) to confirm deletion.  If Ok is pressed, then it removes the activity from the
             * database.
             * @param activityId Id of the activity to delete
             * @returns {Promise<void>}
             */
            async deleteActivity(activityId) {
                this.errored = false;
                let confirmDeleteActivity = false;

                // Open dialog box
                await this.$bvModal.msgBoxConfirm("Are you sure you want to delete this Activity?")
                    .then(value => {
                        confirmDeleteActivity = value
                    })
                    .catch(() => {
                        this.errored = true;
                        this.error_message = "Could not delete activity";
                    });
                if (!confirmDeleteActivity) {
                    return;
                }

                // Delete from local memory
                this.activityList.splice(this.activityList.findIndex(a => a.id === activityId), 1);

                // Delete from database
                await api.deleteActivity(this.userId, activityId).catch(() => {
                    this.errored = true;
                    this.error_message = "Could not delete activity";
                })
            },


            /**
             * Iterates over list of activities
             * and obtains name of creator of an activity.
             * Then manually assigns a "creatorName" property to each activity.
             */
            async getCreatorNamesForActivities() {
                for (let i = 0; i < this.activityList.length; i++) {
                    await api.getUserData(this.activityList[i].creatorUserId).then((response) => {
                        this.activityList[i]["creatorName"] = `${response.data.firstname} ${response.data.lastname}`;
                    }).catch((error) => {
                        this.processGetError(error, "UNKNOWN")
                    });
                }
            },

            /**
             * Iterates over list of activities
             * and obtains a follow status
             * for an activity that a user is following
             */
            async getFollowingStatusForActivity() {
                for (let i = 0; i < this.activityList.length; i++) {
                    if (this.activeUserId !== this.activityList[i].creatorUserId) {
                        await api.getUserSubscribed(this.activityList[i].id, this.activeUserId).then((response) => {
                            this.activityList[i]["subscribed"] = response.data.subscribed;
                        }).catch((error) => {
                            this.processGetError(error, "UNKNOWN")
                        });
                    }
                }
                this.loading = false;
            },


            /**
             * Called when activity modal is triggered to close (x button in modal).
             * If the ActivityList component belongs to a User
             * and the activity in question has been UNFOLLOWED,
             * then the activity is removed from activityList.
             * @param filteredIndex of activity in modal
             * @param isContinuous Type of Activity to follow
             * @returns {Promise<void>}
             */
            async onHidden (filteredIndex, isContinuous) {
                if (this.user_Id === this.activeUserId) {
                    // if this is my login page
                    let activity = this.activityList.filter(activity => activity.continuous === isContinuous)[filteredIndex];
                    let index = this.activityList.indexOf(activity);
                    if (this.activityList[index].subscribed === false) {
                        this.activityList.splice(index, 1);
                    }
                }
                this.$forceUpdate();  // Notice we have to use a $ here
            },


            /**
             * Logs the user out and clears session token
             */
            logout () {
                api.logout()
                sessionStorage.clear();
                this.isLoggedIn = (sessionStorage.getItem("token") !== null);
                this.$forceUpdate();
                this.$router.push('/login');
            },
        }
    }


</script>

<style scoped>
    .footSteps {
        width: 7.5%;
        height: 7.5%;
    }
    .noMore {
        text-align: center;

    }
    .text-justified {
        text-align: justify;
    }

    .activity-button-group {
        padding: 7.5% 30px;
    }

    .activity-button-group button {
        width: 190px;
    }

    .continuousCard {
        margin-bottom: -5px;
    }

    .durationCard {
        margin-bottom: 20px;
    }
</style>