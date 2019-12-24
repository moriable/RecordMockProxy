Vue.filter('timeformat', function (timestamp) {
    return moment(new Date(timestamp)).format('HH:mm:ss.SSS');
})

Vue.component('body-content', {
    props: ['targetid', 'contenttype', 'type'],
    template: `<span v-if="isRaw" v-html="body"></span>
        <span v-else>{{body}}</span>`,

    data: function() {
        return {
            isRaw: false,
            body: ''
        }
    },
    watch: {
        targetid: function() {
            this.isRaw = this.contenttype.startsWith('image/');
            if (this.isRaw) {
                this.body = `<img src="/api/record/${this.targetid}/${this.type}">`;
            } else {
                axios.get(`/api/record/${this.targetid}/${this.type}`)
                    .then((response) => {
                        this.body = response.data;
                    })
                    .catch((error) => {
                        this.body = '';
                        console.log(error);
                    });
            }
        }
    }
});

const Record = {
    template: '#record',
    data: function() {
        return {
            recordRows: [], // record id list
            records: new Map(),
            detail: false,
            selectId: -1,
            detailTab: 'request'
        }
    },
    created: function() {
        console.log('Record.created');
        axios.get('/api/record')
            .then((response) => {
                response.data.forEach((record) => {
                    console.log(this);
                    this.recordRows.push(record.id);
                    this.records[record.id] = record;
                });
            })
            .catch((error) => {
                console.log(error);
            });

        const socket = new WebSocket(`ws://${location.host}/api/websocket`);
        socket.addEventListener('open', event => {
        });
        socket.addEventListener('message', event => {
            var recordData = JSON.parse(event.data);
            console.log(recordData.data.id);
            if (this.recordRows.indexOf(recordData.data.id) < 0) {
                this.recordRows.push(recordData.data.id);
            }
            this.records[recordData.data.id] = recordData.data;
        });
    },
    methods: {
        showDetail: function(i) {
            this.detail = true;
            this.selectId = i;
        },
        closeDetail: function(event) {
            this.detail = false;
            this.selectId = -1;
        },
        createMock: function() {
            console.log('createMock');
            axios.post(`/api/record/${this.selectId}/mock`)
                .then((response) => {
                    console.log(response);
                    router.push({path:'mock', query: { id: response.data.target.id }});
                })
                .catch((error) => {
                    console.log(error);
                    alert(error);
                });
        }
    }
}
const Mock = {
    template: '#mock',
    created: function() {
        this.selectId = this.$route.query.id;

        axios.get('/api/mock')
            .then((response) => {
                Object.entries(response.data).forEach(([key, value]) => {
                    if (!this.selectId) {
                        console.log(value);
                        this.selectId = value.target.id;
                    }
                    this.mocks.push(value);
                })
            })
            .catch((error) => {
                console.log(error);
            });
    },
    data: () => {
        return {
            mocks: [],
            selectId: ''
        }
    },
    methods: {
        showDetail: function(id) {
            this.selectId = id;
        }
    }
}

const router = new VueRouter({
    routes: [
        { path: '/record', component: Record, props: true },
        { path: '/mock', component: Mock, props: true }
    ]
})

const app = new Vue({
    router
}).$mount('#app')

var getRecord = () => {

};
getRecord();