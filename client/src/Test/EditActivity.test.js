import 'vue-jest'
import {shallowMount} from '@vue/test-utils'
import EditActivity from "../views/Activities/EditActivity";
import api from "../Api";
import router from '../index';
jest.mock("../Api");

let editActivity;
let config;
const DEFAULT_USER_ID = 1;

beforeAll(() => {
    config = {
        router
    };
    // This Removes: TypeError: Cannot read property 'then' of undefined
    api.getUserId.mockImplementation(() => Promise.resolve({ data: DEFAULT_USER_ID, status: 200 }));
    editActivity = shallowMount(EditActivity, config);
});

const ACTIVITY1 = {
    activity_name: "Trail Run Arthur's Pass",
    description: "A trail run of Mingha - Deception Route in Arthur's pass.  South to north.",
    activity_type: ["Astronomy", "Hiking"],
    continuous: false,
    location: "Arthur's Pass National Park",
    start_time: "2020-12-16T09:00:00+0000",
    end_time: "2020-12-17T17:00:00+0000"
};

test('Is a vue instance', () => {
    expect(editActivity.isVueInstance).toBeTruthy();
});

test('Catches an http status error of 401 or user not authenticated when edit activity form is submitted and takes user to login page', () => {
    editActivity.setProps({
        ACTIVITY1
    });

    let networkError = new Error("Mocked Network Error");
    networkError.response = {status: 401};   // Explicitly give the error a response.status
    api.updateActivity.mockImplementation(() => Promise.reject(networkError));  // Mocks errors sent from the server
    let spy = jest.spyOn(router, 'push');

    return editActivity.vm.submitEditActivity().then(() => {
        expect(spy).toHaveBeenCalledWith('/login');
    });
});


test('Catches an http status error of 403 or forbidden access when edit activity form is submitted and gives user an appropriate alert', () => {
    editActivity.setProps({
        ACTIVITY1
    });

    let networkError = new Error("Mocked Network Error");
    networkError.response = {status: 403};   // Explicitly give the error a response.status
    api.updateActivity.mockImplementation(() => Promise.reject(networkError));  // Mocks errors sent from the server

    return editActivity.vm.submitEditActivity().catch(
        error => expect(error).toEqual(new Error("Sorry unable to edit this activity (forbidden access)")));
});

test('Checking 200 response status of edit activity function', () => {
    editActivity.setProps({
        ACTIVITY1
    });


    api.updateActivity.mockImplementation(() => Promise.resolve({ data: {'Token': 'ValidToken', 'userId': 1}, status: 200 }));  // Mocks errors sent from the server
    let spy = jest.spyOn(router, 'push');

    return editActivity.vm.submitEditActivity().then(() => {
        expect(spy).toHaveBeenCalledWith({name: 'allActivities', params: {alertMessage: 'Activity added successfully', alertCount: 5}});
    })

});

test('Catches an http status error that isnt 401, 404, 403 and gives the user an appropriate alert', () => {
    editActivity.setProps({
        ACTIVITY1
    });

    let networkError = new Error("Mocked Network Error");
    networkError.response = {status: 408};   // Explicitly give the error a response.status
    api.updateActivity.mockImplementation(() => Promise.reject(networkError));  // Mocks errors sent from the server

    return editActivity.vm.submitEditActivity().catch(
        error => expect(error).toEqual(new Error("Unknown error has occurred whilst editing this activity")));
});

test('Catches an http status error of 401 when activity data is coming back from the server get endpoint', () => {
    editActivity.setProps({
        ACTIVITY1
    });

    let networkError = new Error("Mocked Network Error");
    networkError.response = {status: 401};   // Explicitly give the error a response.status
    api.getActivityData.mockImplementation(() => Promise.reject(networkError));  // Mocks errors sent from the server
    let spy = jest.spyOn(router, 'push');


    return editActivity.vm.getActivityData().then(() => {
        expect(spy).toHaveBeenCalledWith('/login');
    });
});

test('Catches an http status error that isnt 401 when activity data is coming back from the server get endpoint', () => {
    editActivity.setProps({
        ACTIVITY1
    });

    let networkError = new Error("Mocked Network Error");
    networkError.response = {status: 408};   // Explicitly give the error a response.status
    api.getActivityData.mockImplementation(() => Promise.reject(networkError));  // Mocks errors sent from the server
    let spy = jest.spyOn(router, 'push');


    return editActivity.vm.getActivityData().then(() => {
        expect(spy).toHaveBeenCalledWith({ name: 'myProfile' });
    });
});

