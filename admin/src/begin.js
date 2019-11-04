import React, {Component} from 'react';
import logo from './trviavetlogo.png';

import MyForm from './createAccount';
import Login from './Login';
import "./App.css";


  export default class Begin extends Component {
      state = {
        contacts: [],
        users: []
      }


      addUser = (username, password, firstName, lastName, dni) => {
         //console.log("adding a new user...");
         const newUser = {
            username : username,
            password : password,
            firstName : firstName,
            lastName : lastName,
            dni : dni
         }
         this.setState ({
            users  : [...this.state.users, newUser]
         })
         console.log(this.state.users);
      }

      render () {
        return (
          <div>
            <div className="App-header">
            <center><h1>Welcome to TriviaVet</h1></center>
            <MyForm addUser={this.addUser} />
            <img className="begin-logo" src={logo} alt="logo"/>
            <Login/>
            </div>
          </div>

    );
  }
}
