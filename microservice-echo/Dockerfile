FROM openmatchmaking/docker-base-python-image:3.7

RUN mkdir /code
COPY microservice-echo/ /code
WORKDIR /code

RUN pip3 install -r requirements.txt
