# Makefile for backend

run:
	poetry run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

test:
	poetry run pytest -v

shell:
	poetry shell

lock:
	poetry lock
	poetry install

migrate:
	poetry run alembic upgrade head

makemigration:
	poetry run alembic revision --autogenerate -m "$(name)"
