const Record = {
    template: '#record',
    data: () => {
        return {
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
        { path: '/record', component: Record },
        { path: '/mock', component: Mock }
    ]
})

const app = new Vue({
    router
}).$mount('#app')