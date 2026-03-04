import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';

interface LoginPayload {
  email: string;
  password: string;
}

@Injectable()
export class AuthService {
  private readonly demoUser = {
    email: 'admin@example.com',
    password: 'password123',
    name: 'Demo Admin',
  };

  constructor(private readonly jwtService: JwtService) {}

  validateUser({ email, password }: LoginPayload) {
    if (email === this.demoUser.email && password === this.demoUser.password) {
      const { password: _, ...safeUser } = this.demoUser;
      return safeUser;
    }
    throw new UnauthorizedException('Invalid credentials');
  }

  login(payload: LoginPayload) {
    const user = this.validateUser(payload);
    const accessToken = this.jwtService.sign({ sub: user.email, email: user.email });
    return { accessToken, user };
  }
}
