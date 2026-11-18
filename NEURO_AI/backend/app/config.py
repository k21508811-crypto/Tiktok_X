from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    ai_base_url: str = "https://api.openai.com/v1"
    ai_api_key: str = ""
    ai_model: str = ""
    ai_timeout_seconds: float = 60.0
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()
