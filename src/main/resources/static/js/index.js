Vue.filter('timeformat', function (timestamp) {
    return moment(new Date(timestamp)).format('HH:mm:ss.SSS');
})

Vue.component('body-content', {
    props: ['targetid', 'contenttype', 'type'],
    template: '<span v-html="body"></span>',
    data: function() {
        return {
            body: ''
        }
    },
    created: function() {
        console.log(this.targetid, this.contenttype, this.type);
        if (this.contenttype.startsWith('image/')) {
            this.body = `<img src="/api/record/${this.targetid}/${this.type}">`;
        } else {
            axios.get(`/api/record/${this.targetid}/${this.type}`)
                .then((response) => {
                    this.body = `<pre>${response.data}</pre>`
                })
                .catch((error) => {
                    console.log(error);
                });
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
        }
    }
}
const Mock = {
    template: '<div>mock</div>',
    data: () => {
        return {
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