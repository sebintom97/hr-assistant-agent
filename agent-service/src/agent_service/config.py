import http
from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """All agent-service settings. Read from environment variables or .env, and validated at startup."""

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    anthropic_api_key: SecretStr                    
    anthropic_model: str = "claude-haiku-5-5"
    hr_core_url: str = "http://127.0.0.1:8080"
    hr_core_timeout_seconds: float = 10
    max_agent_steps: int = 10
settings = Settings()