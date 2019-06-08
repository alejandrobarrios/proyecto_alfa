import React from 'react';
import {
  AsyncStorage,
  View,
  Image,
  Text,
  TextInput,
  Button,
  TouchableOpacity,
  StyleSheet,
  ImageBackground
} from 'react-native';
import axios from 'axios';

export default class SignInScreen extends React.Component {
  static navigationOptions = {
    title: '¡Bienvenido a TriviaVet!!',
  };

  constructor(props) {
    super(props);
    this.state = {
      username: '',
      password: ''
    }
  }

  render() {
    return (
      
      <View style={styles.container}>
       

         <ImageBackground
            source={
            __DEV__
                ? require('../assets/images/wallpaper.jpg')
                : require('../assets/images/wallpaper.jpg')
            }
            style={styles.welcomeImage}
        >
        </ImageBackground>
      <View style={styles.acomodar}>
        <Text style={styles.getStartedText}>
          Ingresar
        </Text>
      
        <TextInput
          placeholder="Usuario"
          style={styles.input}
          onChangeText={(value) => this.setState({ username: value })}
          value={this.state.username}
        />

        <TextInput
          placeholder="Contraseña"
          style={styles.input}
          secureTextEntry={true}
          onChangeText={(value) => this.setState({ password: value })}
          value={this.state.password}
        />

        <View style={styles.button}>
          <Button title="Sign in" onPress={this._signIn} />
        </View>
          
        <View style={styles.button}>  
          <Button title="Create Account" onPress={this._handleCreateAccount} />
        </View>

        <View style={styles.button}>
          <Button title="Instructions" onPress={this._Instructions} />
        </View>
      </View>
      
      </View>
      
      
    );
  }

  _handleCreateAccount = async () => {
    await AsyncStorage.clear();
    this.props.navigation.navigate('Create');
  };

  _signIn = () => {
    const { username, password } = this.state;

    axios.post("http://192.168.0.31:4567/login", {
      username: username,
      password: password,
    }, {
      auth: {
        username: username,
        password: password
      }
    })
      .then(response => JSON.stringify(response))
      .then(response => {
        // Handle the JWT response here
        AsyncStorage.setItem('userToken', response.data);
        this.props.navigation.navigate('App');
      })
    .catch((error) => {
      if(error.toString().match(/401/)) {
        alert("Username o Password incorrecto");
        return;
      }

      alert("Networking Error");
    });
  };
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    backgroundColor: '#885110',
  },
  welcomeImage: {
    width: '110%',
    height: '110%',
    resizeMode: 'contain',
    marginTop: 3,
    marginLeft: 0,

  },
  welcomeContainer: {
    alignItems: 'center',
    marginTop: 10,
    marginBottom: 80,
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  input: {
    margin: 15,
    marginTop: -10,
    height: 30,
    padding: 5,
    fontSize: 16,
    marginBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#4228F8'
  },
  getStartedText: {
    marginTop: -150,
    fontSize: 17,
    color: '#FFFFFF',
    lineHeight: 24,
    textAlign: 'center',
  },
  button: {
    margin:10,
  },
  acomodar:{
    marginTop: -500,
  }
})
