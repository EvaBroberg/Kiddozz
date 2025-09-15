from fastapi import FastAPI
from app.api import health, events

app = FastAPI(
    title="Kiddozz Backend API",
    version="1.0.0"
)

# Register routers
app.include_router(health.router)
app.include_router(events.router)


@app.get("/")
def read_root():
    """Root endpoint"""
    return {"message": "Welcome to Kiddozz API", "version": "1.0.0", "docs": "/docs"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
