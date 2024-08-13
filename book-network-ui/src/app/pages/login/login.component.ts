import { Component } from '@angular/core';
import {AuthenticationRequest} from "../../services/models/authentication-request";
import {NgIf} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {register} from "../../services/fn/authentication/register";

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    NgIf,
    FormsModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  authRequest: AuthenticationRequest = {email: '', password: ''};
  errorMsg: Array<string> = [];

  login() {

  }

  register() {

  }
}
