Vue.filter('timeformat', function (timestamp) {
    return moment(new Date(timestamp)).format('HH:mm:ss.SSS');
})

const Record = {
    template: '#record',
    data: function() {
        return {
            records: [],
            detail: false,
            selectIndex: -1,
            detailTab: 'request'
        }
    },
    created: function() {
        console.log('Record.created');
        axios.get('/api/record')
            .then((response) => {
                this.records = response.data;
                console.log(response);
            })
            .catch((error) => {
                console.log(error);
            });

        const socket = new WebSocket(`ws://${location.host}/api/websocket`);
        socket.addEventListener('open', function (event) {
        });
        socket.addEventListener('message', function (event) {
            console.log('Message from server ', event.data);
        });
    },
    methods: {
        showDetail: function(i) {
            this.detail = true;
            this.selectIndex = i;
        },
        closeDetail: function(event) {
            this.detail = false;
            this.selectIndex = -1;
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