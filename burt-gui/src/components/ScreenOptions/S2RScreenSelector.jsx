import React , { useState, useEffect }  from "react";
import ImagePicker from './../ImagePicker/ImagePicker'
import "./AppSelector.css";
import ApiClient from "../../ApiClient";
import processResponse from "../../ServerResponseProcessor";


let logos = require.context('../../../../data/app_logos', true);

const S2RScreenSelector = (props) => {

    const [screen, setScreen] = useState({});
    // const [imageChanged, setImageChanged] = useState(false);

    const pickImageHandler = (image) => {
        setScreen(image);

    }
    const handleConfirmButton= (choice) => {

        setTimeout(() => {
            let selectedValues;
            if (!multiple){
                selectedValues = [screen.value];
            }else{
                selectedValues = screen.map(s => s.value);
            }
            let message = props.actionProvider.createChatBotMessage(choice, {selectedValues: selectedValues}); // why this message does not show up on the chatbot?
            const responsePromise = ApiClient.processUserMessage(message)
            processResponse(responsePromise, props.actionProvider)

            const idx = props.messages.findIndex(x => x.id === props.id)

            props.messages[idx].selectedValues = selectedValues;

        }, 1000)
    }
    const handleNegativeButton = (choice)=>{
        setTimeout(() => {

            let message = props.actionProvider.createChatBotMessage(choice);

            const responsePromise = ApiClient.processUserMessage(message)
            processResponse(responsePromise, props.actionProvider)

        }, 1000)
    }


    const getImageStyle = (width, height) => {
        return {
            width,
            height,
            objectFit: "cover"
        }
    }
    const dataValues = props.allValues; // only one screenshot
    const selectedValues = props.selectedValues
    const multiple = props.multiple;

    return (
        <div>
            <ImagePicker
                images={dataValues.map((image, i) => ({src: logos("./" + image.value).default, value: image.key}))}
                style={getImageStyle(150, 300)}
                selected={selectedValues}
                onPick={pickImageHandler}
                multiple = {multiple}
                // onClick={this.props.actionProvider.handleOneScreenOption()}/>;
            />
            <center>
                <button type="button" className="button" onClick={() => handleConfirmButton("done")}>done</button>
                <button type="button" className="button" onClick={() => handleNegativeButton("none of above")}>none of above</button>
            </center>


        </div>
    )

}


export default S2RScreenSelector;