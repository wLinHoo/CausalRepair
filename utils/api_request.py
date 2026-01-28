# utils/api_request.py

import requests
import time
import json
from openai import OpenAI

# ======================================================================
# API Client Configuration
# ======================================================================

# --- SiliconFlow ---
SILICONFLOW_API_KEY = ""
SILICONFLOW_URL = "https://api.siliconflow.cn/v1/chat/completions"

# --- OpenAI ---
OPENAI_CLIENT = OpenAI(
    api_key = ""
)

# --- ZHIPU AI ---
ZHIPU_API_KEY = "" 
ZHIPU_URL = ""

# ======================================================================

def create_request_config(prompt, provider, model, max_tokens=4096, temperature=1.0):
    """
    Create a configuration dictionary containing all parameters required for a request.
    """
    return {
        "prompt": prompt,
        "provider": provider,
        "model": model,
        "max_tokens": max_tokens,
        "temperature": temperature,
    }

def _request_siliconflow(config):
    """
    Handle requests to the SiliconFlow API.
    """
    headers = {
        "Authorization": f"Bearer {SILICONFLOW_API_KEY}",
        "Content-Type": "application/json"
    }
    payload = {
        "model": config['model'], 
        "messages": [
            {"role": "system", "content": "You are a helpful assistant that fixes Java code."},
            {"role": "user", "content": config['prompt']}
        ],
        "max_tokens": config['max_tokens'],
        "temperature": config['temperature'],
        "stream": False
    }
    proxies = {"http": None, "https": None}

    try:
        response = requests.post(SILICONFLOW_URL, json=payload, headers=headers, timeout=180, proxies=proxies)
        response.raise_for_status()
        response_json = response.json()

        content = response_json.get('choices', [{}])[0].get('message', {}).get('content', '')
        usage = response_json.get('usage', {})  
        return content, usage

    except requests.exceptions.RequestException as e:
        print(f"An error occurred during the SiliconFlow API request: {e}")
        return "", {}
    except (KeyError, IndexError) as e:
        print(f"An error occurred while parsing the SiliconFlow API response: {e}")
        return "", {}

def _request_openai(config):
    """
    Handle requests to an OpenAI-compatible API.
    """
    try:
        response = OPENAI_CLIENT.chat.completions.create(
            model=config['model'], 
            messages=[
                {"role": "system", "content": "You are a helpful assistant that fixes Java code."},
                {"role": "user", "content": config['prompt']}
            ],
            temperature=config['temperature'],
            max_tokens=config['max_tokens']
        )

        content = response.choices[0].message.content
        usage = response.usage.model_dump() if response.usage else {}
        return content, usage

    except Exception as e:
        print(f"An error occurred during the OpenAI API request: {e}")
        return "", {}

def _request_zhipu(config):
    """
    Handle requests to the Zhipu AI API.
    """
    headers = {
        "Authorization": f"Bearer {ZHIPU_API_KEY}",
        "Content-Type": "application/json"
    }
    payload = {
        "model": config['model'], 
        "messages": [
            {"role": "user", "content": config['prompt']}
        ],
        "temperature": config['temperature'] if config['temperature'] > 0 else 0.01,
    }

    try:
        response = requests.post(ZHIPU_URL, json=payload, headers=headers, timeout=180)
        response.raise_for_status()
        response_json = response.json()

        content = response_json.get('choices', [{}])[0].get('message', {}).get('content', '')
        usage = response_json.get('usage', {}) 
        return content, usage

    except requests.exceptions.RequestException as e:
        print(f"An error occurred during the Zhipu AI API request: {e}")
        return "", {}
    except (KeyError, IndexError) as e:
        print(f"An error occurred while parsing the Zhipu AI API response: {e}")
        return "", {}

def request_engine(config):
    """
    Main request function. Selects the corresponding handler based on the API provider.
    """
    provider = config.get("provider")
    print(f"      (Using Provider: {provider}, Model: {config.get('model')})")

    if provider == 'siliconflow':
        return _request_siliconflow(config)
    elif provider == 'openai':
        return _request_openai(config)
    elif provider == 'zhipu':
        return _request_zhipu(config)
    else:
        print(f"Error: Unknown API provider '{provider}'.")
        return "", {}
