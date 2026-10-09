export interface AuthDto {
    username: string,
    password: string;
}

export interface AuthResponse {
    token: string;
}